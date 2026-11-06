package dev.plugdesk.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * What the application on the desk says it is, in {@code META-INF/plugdesk/application.properties}: its name, the
 * folder in the person's home where it keeps their things, the repository whose releases are its plugins (and the one
 * they were published from before, if they moved), how the jars of its plugins start, and what it calls itself when
 * it asks for a file, the look it starts with, the order of its menus, the menus it moves (a key {@code menu.From/Item}
 * with the path it goes to instead), and whatever else a plugin asks of it by name ({@link #said}).
 */
public record Application(String name, String home, String releases, String formerly, List<String> plugsIn,
                          String asking, String look, List<String> menus, Map<String, String> moves, Properties all) {

  static final String DECLARED = "META-INF/plugdesk/application.properties";

  private static Application it;

  public static synchronized Application it() {
    if (it == null) it = read();
    return it;
  }

  private static List<String> listed(String commas) {
    return Arrays.stream(commas.split(",")).map(String::trim).filter(one -> !one.isEmpty()).toList();
  }

  private static Application read() {
    Properties said = new Properties();
    try (InputStream in = Application.class.getClassLoader().getResourceAsStream(DECLARED)) {
      if (in != null) said.load(in);
    } catch (IOException unreadable) {
      throw new UncheckedIOException(unreadable);
    }
    String name = said.getProperty("name", "plugdesk");
    return new Application(name, said.getProperty("home", "." + name), said.getProperty("releases"),
        said.getProperty("releases.formerly"),
        listed(said.getProperty("plugs-in", "")),
        said.getProperty("asking", name), said.getProperty("look", "Metal"), listed(said.getProperty("menus", "")),
        said.stringPropertyNames().stream().filter(key -> key.startsWith("menu.")).sorted()
            .collect(java.util.stream.Collectors.toMap(key -> key.substring("menu.".length()), said::getProperty,
                (one, other) -> one, java.util.LinkedHashMap::new)), said);
  }

  /** What the application says under that key, or null. */
  public String said(String key) {
    return all.getProperty(key);
  }

  /** The path an action goes to: moved when the application moves it or a menu it is in, else where it said. */
  public String placed(String path) {
    return moves.entrySet().stream().filter(move -> path.equals(move.getKey()) || path.startsWith(move.getKey() + "/"))
        .findFirst().map(move -> move.getValue() + path.substring(move.getKey().length())).orElse(path);
  }
}
