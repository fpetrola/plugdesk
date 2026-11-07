package dev.plugdesk.standard.help;

import dev.plugdesk.app.Application;
import dev.plugdesk.desk.Action;
import dev.plugdesk.desk.Actions;
import dev.plugdesk.desk.Workbench;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import java.util.List;
import java.util.stream.Stream;

/**
 * What the application is, as it says in its own file, and every key and what it does. The application may say where
 * the keys go and which keys show them ({@code keymap} and {@code keymap.keys} in its file): an IDE's Keymap Reference.
 */
public class Help implements Actions {

  @Override
  public List<Action> actions() {
    String keymap = Application.it().said("keymap");
    String keys = Application.it().said("keymap.keys");
    return List.of(new Doing(keymap == null || keymap.isBlank() ? "Help/Keys" : keymap, this::keys,
        keys == null || keys.isBlank() ? List.of() : List.of(KeyStroke.getKeyStroke(keys))), new Doing("Help/About", this::about, List.of()));
  }

  private record Doing(String path, Runnable doing, List<KeyStroke> keys) implements Action {
    public void run() {
      doing.run();
    }

    public int place() {
      return 1000;
    }
  }

  /** The name, version and description, and then whoever made it, under which license and where it lives. */
  String aboutText() {
    Application application = Application.it();
    StringBuilder text = new StringBuilder(title());
    if (application.said("version") != null) text.append(" ").append(application.said("version"));
    if (application.said("description") != null) text.append("\n").append(application.said("description"));
    text.append("\n");
    for (String said : List.of("author", "license", "website", "copyright"))
      if (application.said(said) != null) text.append("\n").append(Character.toUpperCase(said.charAt(0)))
          .append(said.substring(1)).append(": ").append(application.said(said));
    return text.toString();
  }

  private void about() {
    JOptionPane.showMessageDialog(Workbench.current(), aboutText(), "About " + title(), JOptionPane.INFORMATION_MESSAGE);
  }

  /** What the application is called where a person reads it: its title, or its name when it gives none. */
  private static String title() {
    String title = Application.it().said("title");
    return title != null ? title : Application.it().name();
  }

  /**
   * Every action that has keys, by where it is in the menus, with all its keys and what it does, when it says: menu by
   * menu, as the bar has them, and in each as its items go.
   */
  List<String[]> keysOf(Workbench desk) {
    List<String> bar = new java.util.ArrayList<>();
    for (int i = 0; i < desk.menus().getMenuCount(); i++) bar.add(desk.menus().getMenu(i).getText());
    java.util.function.ToIntFunction<Action> menu = action -> {
      String top = Application.it().placed(action.path()).split("/")[0];
      return bar.contains(top) ? bar.indexOf(top) : bar.size();
    };
    return desk.offered().stream().filter(action -> !action.keys().isEmpty())
        .sorted(java.util.Comparator.comparingInt(menu))
        .map(action -> new String[]{Application.it().placed(action.path()).replace("/", " > "),
            String.join(", ", action.keys().stream().map(Help::said).toList()),
            action.description() == null ? "" : action.description()})
        .toList();
  }

  private void keys() {
    Workbench desk = Workbench.current();
    JTable table = new JTable(keysOf(desk).toArray(String[][]::new), new String[]{"What", "Keys", "What it does"});
    table.setDefaultEditor(Object.class, null);
    JOptionPane.showMessageDialog(desk, new JScrollPane(table), "Keys", JOptionPane.PLAIN_MESSAGE);
  }

  private static String said(KeyStroke key) {
    return Stream.of(key.toString().replace("pressed ", "").split(" "))
        .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1)).reduce((a, b) -> a + "+" + b).orElse("");
  }
}
