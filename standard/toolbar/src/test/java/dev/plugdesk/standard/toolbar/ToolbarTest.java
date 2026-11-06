package dev.plugdesk.standard.toolbar;

import dev.plugdesk.desk.Action;
import dev.plugdesk.desk.Mdi;
import dev.plugdesk.desk.Workbench;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolbarTest {

  record Offering(String path, Icon icon, String needs) implements Action {
    public void run() {
    }
  }

  private static final Icon DOT = new ImageIcon(new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB));

  @Test
  void aButtonForWhatHasAnIconAndAPlusForWhatNeedsAPlugin() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Toolbar toolbar = new Toolbar();
      Workbench desk = new Workbench(new Mdi(), List.of(new Offering("A/With an icon", DOT, null),
          new Offering("A/Without", null, null), new Offering("Discover/Map", DOT, "tool-map")), List.of(), List.of(toolbar));
      JComponent bar = bar(desk);

      assertEquals(List.of("With an icon", "More that can be had: choosing one brings its plugin"),
          Stream.of(bar.getComponents()).map(button -> ((JButton) button).getToolTipText()).toList());
    });
  }

  @Test
  void theBarGoesWhenItsPluginGoes() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      java.util.Set<dev.plugdesk.desk.Part> parts = new java.util.HashSet<>(List.of(new Toolbar()));
      Workbench desk = new Workbench(new Mdi(), List.of(new Offering("A/B", DOT, null)), List.of(), parts);
      assertTrue(bar(desk) != null, "on the desk");

      parts.clear();
      desk.changed();

      assertEquals(null, bar(desk));
    });
  }

  /** The bar on the desk, wherever the desk puts it; none if it is not there. */
  private static JComponent bar(java.awt.Container on) {
    for (java.awt.Component inside : on.getComponents()) {
      if (inside instanceof javax.swing.JToolBar bar) return bar;
      if (inside instanceof java.awt.Container container && bar(container) != null) return bar(container);
    }
    return null;
  }
}
