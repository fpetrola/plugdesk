/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.plugdesk.app;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The boards that are published, and bringing one in.
 * <p>
 * Each is a release of its own with one jar in it, so the list is a question to an archive and
 * taking one is a download. Nothing here decides to do either: somebody asks for the list and
 * picks what to add, which is the only consent a download needs.
 */
@Section("plugins")
public class Releases implements Configuration.Saves {

  private static final String WHERE_THEY_ARE = Application.it().releases();

  /**
   * Where they used to be published, before they were a repository of their own. Somebody who
   * ran this before that has it written down in their settings, and settings are read over the
   * default: without this they would go on asking the emulator's own releases, which no longer
   * carry any, and see an empty list with nothing saying why.
   */
  private static final String WHERE_THEY_USED_TO_BE = Application.it().formerly();

  /** Where the boards are published: the releases of this repository whose tag names a device. */
  public String repository = WHERE_THEY_ARE;

  /** What this emulator calls itself when it asks somebody for a file. */
  private static final String WHO_IS_ASKING = Application.it().asking();

  /** What a tag or a jar is called when it is something to plug in: a board, or a tool over one. */
  private static final List<String> WHAT_PLUGS_IN = Application.it().plugsIn();

  private static boolean plugsIn(String named) {
    return WHAT_PLUGS_IN.isEmpty() || WHAT_PLUGS_IN.stream().anyMatch(named::startsWith);
  }

  private static final Duration PATIENCE = Duration.ofSeconds(15);

  private Configuration configuration;

  @Override
  public void savedBy(Configuration configuration) {
    this.configuration = configuration;
    if (WHERE_THEY_USED_TO_BE != null && WHERE_THEY_USED_TO_BE.equals(repository)) {
      repository = WHERE_THEY_ARE;
      configuration.save();
    }
  }

  private static Releases theOne() {
    return Configuration.shared().of(Releases.class);
  }

  /**
   * One published board: what it is called, which file it is, where it is, how big, and where
   * what it answers for is published, so that it can be asked without bringing the jar.
   */
  public record Board(String name, String jar, String from, long asset, long size,
                      String sha256, String metadata) {
    public Board(String name, String jar, String from, long asset, long size) {
      this(name, jar, from, asset, size, null, null);
    }
  }

  /**
   * The boards that would answer for this under any of these roles: a machine as a snapshot
   * names it, or a kind of file. Each board says it itself, in what it was compiled with, and
   * comes from wherever the plugins are offered: the ones the app carries count as much as the
   * published ones.
   */
  public static List<Board> bringing(String wanted, List<String> roles) {
    return roles.stream()
        .flatMap(role -> Plugins.managing().availableAnswering(role, wanted).stream())
        .map(artifact -> new Board(artifact.id(), artifact.id() + "-" + artifact.version() + ".jar", null, 0, 0))
        .distinct().toList();
  }

  /**
   * Which board a jar is, whatever version it happens to be: "tool-calls-0.0.2-SNAPSHOT.jar"
   * and "tool-calls-0.0.3.jar" are both "tool-calls". A board nobody published is called this
   * too, since its file is all there is to call it after.
   */
  static String whichBoard(String jar) {
    return jar.replaceFirst("-\\d.*$", "").replaceFirst("\\.jar$", "");
  }

  /**
   * The answer of a moment ago, rather than the same download again: the list is asked for every
   * time the window is opened or told to look again, and it only changes when a build runs.
   */
  private static List<Board> lastAnswer;
  private static long lastAsked;
  private static final long WORTH_ASKING_AGAIN = java.time.Duration.ofMinutes(5).toMillis();

  private static java.util.concurrent.CompletableFuture<List<Board>> asking;

  /**
   * Lo publicado si ya se sabe, o nada todavia: la pregunta sale en segundo plano y quien espera se
   * entera cuando llega. Asi lo que esta en la maquina se muestra sin esperar a la red.
   */
  static synchronized List<Board> publishedIfKnown() {
    if (lastAnswer != null && System.currentTimeMillis() - lastAsked < WORTH_ASKING_AGAIN) return lastAnswer;
    if (asking == null || asking.isDone()) {
      asking = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
        try {
          return published();
        } catch (IOException | InterruptedException | RuntimeException couldNotAsk) {
          return List.<Board>of();
        }
      });
      asking.thenRun(Plugins::catalogArrived);
    }
    return lastAnswer == null ? List.of() : lastAnswer;
  }

  public static List<Board> published() throws IOException, InterruptedException {
    if (lastAnswer != null && System.currentTimeMillis() - lastAsked < WORTH_ASKING_AGAIN) {
      return lastAnswer;
    }
    String repository = theOne().repository;
    if (repository == null) return List.of();
    HttpResponse<String> answer = client().send(asking(catalogOf(repository)).build(), HttpResponse.BodyHandlers.ofString());
    if (answer.statusCode() != 200) throw new IOException(repository + " answered " + answer.statusCode());

    List<Board> boards = boards(answer.body());
    lastAnswer = boards;
    lastAsked = System.currentTimeMillis();
    return boards;
  }

  /** The releases in the catalog that are something to plug in, each with its jar. */
  static List<Board> boards(String answer) throws IOException {
    List<Board> boards = new ArrayList<>();
    for (JsonNode release : new ObjectMapper().readTree(answer)) {
      String tag = release.path("tag_name").asText("");
      if (!plugsIn(tag)) continue;
      String metadata = null;
      for (JsonNode asset : release.path("assets")) {
        if (asset.path("name").asText("").endsWith("plugin-metadata.json")) {
          metadata = asset.path("browser_download_url").asText();
        }
      }
      for (JsonNode asset : release.path("assets")) {
        String jar = asset.path("name").asText("");
        if (!jar.endsWith(".jar")) continue;
        String named = release.path("name").asText("");
        // El sha256 lo publica el archivo por cada asset, asi que verificar no cuesta una bajada.
        String digest = asset.path("digest").asText("");
        boards.add(new Board(named.isBlank() ? tag : named, jar,
            asset.path("browser_download_url").asText(), asset.path("id").asLong(),
            asset.path("size").asLong(),
            digest.startsWith("sha256:") ? digest.substring("sha256:".length()) : null, metadata));
        break;
      }
    }
    boards.sort(java.util.Comparator.comparing(Board::name));
    return boards;
  }

  /**
   * The list of releases the repository's build publishes as a file, in the shape the archive's API
   * answers. A download and not a question to the API, which allows sixty an hour per address.
   */
  static String catalogOf(String repository) {
    return "https://github.com/" + repository + "/releases/download/catalog/catalog.json";
  }

  /**
   * Brings one in and puts it where the emulator will find it, which is the same as plugging it
   * in: from here on it is one more thing the machine can be built with.
   */
  public static Path bring(Board board) throws IOException, InterruptedException {
    String id = whichBoard(board.jar());
    // Pedirlo por su nombre: quien los carga lo baja del catalogo, verifica el sha256 que el
    // archivo publica, y trae lo que necesite. Bajarlo nosotros y despues mirar de que depende
    // era instalar sin su dependencia, fallar, y hacer falta otra vuelta para cada pieza.
    Plugins.add(id);
    return Plugins.folder().resolve(board.jar());
  }


  /** La descripcion de un release, esperada lo que se espera cualquier respuesta del archivo. */
  static java.util.Optional<dev.crystal.plugins.api.PluginDescription> described(String metadata) {
    try (java.io.InputStream in = client().send(asking(metadata).build(), HttpResponse.BodyHandlers.ofInputStream()).body()) {
      return java.util.Optional.of(dev.crystal.plugins.runtime.PluginSources.description(in));
    } catch (IOException | RuntimeException couldNotAsk) {
      return java.util.Optional.empty();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return java.util.Optional.empty();
    }
  }


  private static HttpClient client() {
    return HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(PATIENCE)
        .build();
  }

  /** Saying who is asking, because an archive that publishes these hangs up on a request that does not. */
  private static HttpRequest.Builder asking(String url) {
    return HttpRequest.newBuilder(URI.create(url)).timeout(PATIENCE).header("User-Agent", WHO_IS_ASKING);
  }
}
