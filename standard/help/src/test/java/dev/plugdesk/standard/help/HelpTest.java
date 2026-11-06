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

  @Test
  void aboutSaysWhatTheApplicationSaysOfItself() {
    assertEquals("Sample 1.0\nWhat it is\n\nAuthor: Somebody\nLicense: GPL v3", new Help().aboutText());
  }

  @Test
  void everyActionWithKeysIsListedWhereItIsInTheMenus() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Workbench desk = new Workbench(new Mdi(), List.of(new Keyed("File/Open...", List.of(KeyStroke.getKeyStroke("ctrl O"),
          KeyStroke.getKeyStroke("F3"))), new Keyed("File/Close", List.of())), List.of());

      assertEquals(List.of("File > Open... | Ctrl+O, F3"),
          new Help().keysOf(desk).stream().map(row -> row[0] + " | " + row[1]).toList());
    });
  }
}
