package dev.plugdesk.standard.help;

import dev.plugdesk.desk.Action;
import dev.plugdesk.desk.Mdi;
import dev.plugdesk.desk.Workbench;
import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelpTest {

  record Keyed(String path, List<KeyStroke> keys) implements Action {
    public void run() {
    }
  }

  record Described(String path, List<KeyStroke> keys, String description) implements Action {
    public void run() {
    }
  }

  @Test
  void aboutSaysWhatTheApplicationSaysOfItself() {
    assertEquals("Sample 1.0\nWhat it is\n\nAuthor: Somebody\nLicense: GPL v3", new Help().aboutText());
  }

  @Test
  void everyActionWithKeysIsListedWhereItIsInTheMenus() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Workbench desk = new Workbench(new Mdi(), List.of(new Keyed("File/Open...", List.of(KeyStroke.getKeyStroke("ctrl O"),
          KeyStroke.getKeyStroke("F3"))), new Keyed("File/Close", List.of())), List.of());

      assertEquals(List.of("File > Open... | Ctrl+O, F3 | "),
          new Help().keysOf(desk).stream().map(row -> String.join(" | ", row)).toList());
    });
  }

  /** What an action does, when it says, goes beside its keys. */
  @Test
  void eachKeySaysWhatItDoes() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Workbench desk = new Workbench(new Mdi(), List.of(new Described("Refactor/Rename...", List.of(KeyStroke.getKeyStroke("shift F6")),
          "Rename the element under the caret")), List.of());

      assertEquals(List.of("Refactor > Rename... | Shift+F6 | Rename the element under the caret"),
          new Help().keysOf(desk).stream().map(row -> String.join(" | ", row)).toList());
    });
  }

  /** The application says where the keys go and which key shows them: an IDE's Keymap Reference. */
  @Test
  void theKeysAreWhereTheApplicationSays() {
    Action keys = new Help().actions().getFirst();

    assertEquals("Help/Keymap Reference", keys.path());
    assertEquals(List.of(KeyStroke.getKeyStroke("ctrl shift L")), keys.keys());
  }

  /** Menu by menu, as the bar has them, not by each action's place: an action of the last menu with a low place goes last. */
  @Test
  void theKeysGoMenuByMenu() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Workbench desk = new Workbench(new Mdi(), List.of(
          new Placed("Help/Find", List.of(KeyStroke.getKeyStroke("ctrl F")), 0),
          new Placed("File/Open...", List.of(KeyStroke.getKeyStroke("ctrl O")), 10),
          new Placed("File/Save", List.of(KeyStroke.getKeyStroke("ctrl S")), 20)), List.of());

      assertEquals(List.of("File > Open...", "File > Save", "Help > Find"),
          new Help().keysOf(desk).stream().map(row -> row[0]).toList());
    });
  }

  record Placed(String path, List<KeyStroke> keys, int place) implements Action {
    public void run() {
    }
  }
}
