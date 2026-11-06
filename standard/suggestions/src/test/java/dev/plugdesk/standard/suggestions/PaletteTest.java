package dev.plugdesk.standard.suggestions;

import dev.plugdesk.desk.*;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaletteTest {

  record Offering(String path, String needs) implements Action {
    public void run() {
    }
  }

  @Test
  void typingLeavesWhatSaysItAndWhatNeedsAPluginSaysWhich() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Workbench desk = new Workbench(new Mdi(), List.of(new Offering("File/Open...", null),
          new Offering("Discover/Open: Map", "tool-map"), new Offering("Help/About", null)), List.of());
      Palette palette = new Palette(desk, () -> { });

      palette.type("open");

      assertEquals(List.of("Open...", "Open: Map   —   needs tool-map"), palette.shown());
    });
  }

  record Described(String path, String description) implements Action {
    public void run() {
    }
  }

  /** What an action does finds it too, as IDEs find an action by any of its words. */
  @Test
  void whatItDoesFindsItToo() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Workbench desk = new Workbench(new Mdi(), List.of(new Described("Refactor/Rename...", "Rename the element under the caret"),
          new Described("File/Save All", "Write the changed files to disk")), List.of());
      Palette palette = new Palette(desk, () -> { });

      palette.type("caret");

      assertEquals(List.of("Rename...: Rename the element under the caret"), palette.shown());
    });
  }

  /** The application says where the palette goes and with which keys: an IDE's is Find Action. */
  @Test
  void thePaletteIsWhereTheApplicationSays() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      new Workbench(new Mdi(), List.of(), List.of());
      Action opening = new Suggestions().actions().getFirst();

      assertEquals("Help/Find Action...", opening.path());
      assertEquals(List.of(javax.swing.KeyStroke.getKeyStroke("ctrl shift A")), opening.keys());
    });
  }
}
