package dev.plugdesk.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * What the application on the desk says it is, in {@code META-INF/plugdesk/application.properties}: its name, the
 * folder in the person's home where it keeps their things, the repository whose releases are its plugins (and the one
 * they were published from before, if they moved), how the jars of its plugins start, and what it calls itself when
 * it asks for a file.
 */
public record Application(String name, String home, String releases, String formerly, List<String> plugsIn,
                          String asking) {

  static final String DECLARED = "META-INF/plugdesk/application.properties";

  private static Application it;

  public static synchronized Application it() {
    if (it == null) it = read();
    return it;
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
        Arrays.stream(said.getProperty("plugs-in", "").split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList(),
        said.getProperty("asking", name));
  }
}
