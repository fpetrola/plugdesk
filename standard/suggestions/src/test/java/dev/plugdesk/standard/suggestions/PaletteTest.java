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
}
