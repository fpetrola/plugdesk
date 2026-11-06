package dev.plugdesk.app;

import dev.crystal.plugins.api.PluginArtifact;
import dev.crystal.plugins.api.PluginDescription;
import dev.crystal.plugins.api.PluginSource;
import dev.crystal.plugins.runtime.PluginSources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * The plugins that come with the application, wherever each lot comes from: the application's own and those of every
 * library it is built on, plugdesk's among them. A jar on the class path that carries plugins brings its index; an
 * application run from its classes (an IDE) says which group and version it bundles, and those are read from the
 * local repository, where a build that has not packaged them yet left them.
 */
final class Defaults implements PluginSource {
  private static final String INDEX = "META-INF/crystal/bundled.idx";
  private static final String UNPACKED = "META-INF/crystal/bundle.properties";

  private final List<PluginSource> sources = new ArrayList<>();
  private final Map<PluginArtifact, PluginSource> from = new LinkedHashMap<>();

  Defaults(ClassLoader loader) {
    try {
      for (URL each : Collections.list(loader.getResources(UNPACKED)))
        if ("file".equals(each.getProtocol())) sources.add(repository(each));
      for (URL each : Collections.list(loader.getResources(INDEX)))
        if (!"file".equals(each.getProtocol()))
          sources.add(PluginSources.bundled(new URLClassLoader(new URL[]{jarOf(each)}, null)));
    } catch (IOException unreadable) {
      TellsThePerson.that("the plugins that come with this application cannot be read: " + unreadable.getMessage());
    }
  }

  /**
   * Installs those that come with the application and were never offered before. The plugin service installs them
   * only the first time it starts, so an application that comes to carry one more would never give it to whoever
   * already ran it; and one somebody took out was offered already, and stays out.
   */
  void offerTheNew(dev.crystal.plugins.runtime.PluginService service) {
    OfferedBefore offered = Configuration.shared().of(OfferedBefore.class);
    try {
      List<String> installed = service.plugins().stream().map(dev.crystal.plugins.runtime.PluginInfo::id).toList();
      boolean any = false;
      for (PluginArtifact artifact : artifacts()) {
        if (offered.ids.contains(artifact.id())) continue;
        if (!installed.contains(artifact.id())) service.install(artifact.id());
        offered.ids.add(artifact.id());
        any = true;
      }
      if (any) Configuration.shared().save();
    } catch (IOException | RuntimeException cannot) {
      TellsThePerson.that("the plugins that come with this application could not all be put in: " + cannot.getMessage());
    }
  }

  /** The plugins that came with the application and were offered already, by id. */
  @Section("plugins.offered")
  public static class OfferedBefore {
    public List<String> ids = new ArrayList<>();
  }

  @Override
  public List<PluginArtifact> artifacts() throws IOException {
    from.clear();
    for (PluginSource source : sources)
      for (PluginArtifact artifact : source.artifacts()) from.putIfAbsent(artifact, source);
    return List.copyOf(from.keySet());
  }

  @Override
  public InputStream open(PluginArtifact artifact) throws IOException {
    PluginSource source = from.get(artifact);
    if (source == null) throw new IOException("Unknown bundled artifact " + artifact.coordinates());
    return source.open(artifact);
  }

  @Override
  public Optional<PluginDescription> describe(PluginArtifact artifact) {
    PluginSource source = from.get(artifact);
    return source == null ? Optional.empty() : source.describe(artifact);
  }

  @Override
  public String origin(PluginArtifact artifact) {
    PluginSource source = from.get(artifact);
    return source == null ? toString() : source.origin(artifact);
  }

  @Override
  public String toString() {
    return "the plugins that come with the application";
  }

  private static PluginSource repository(URL properties) throws IOException {
    Properties bundle = new Properties();
    try (InputStream in = properties.openStream()) {
      bundle.load(in);
    }
    Path repository = Path.of(System.getProperty("maven.repo.local",
        Path.of(System.getProperty("user.home"), ".m2", "repository").toString()));
    return PluginSources.repository(repository, bundle.getProperty("groupId"), bundle.getProperty("version"));
  }

  /** The jar an entry of it was found in. */
  private static URL jarOf(URL entry) throws IOException {
    String spec = entry.toString();
    return new URL(spec.substring("jar:".length(), spec.indexOf("!/")));
  }
}
