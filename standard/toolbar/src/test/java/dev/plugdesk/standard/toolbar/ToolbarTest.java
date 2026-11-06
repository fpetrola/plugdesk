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
      JComponent bar = (JComponent) ((JComponent) desk.getComponent(0)).getComponent(0);

      assertEquals(List.of("With an icon", "More that can be had: choosing one brings its plugin"),
          Stream.of(bar.getComponents()).map(button -> ((JButton) button).getToolTipText()).toList());
    });
  }

  @Test
  void theBarGoesWhenItsPluginGoes() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      java.util.Set<dev.plugdesk.desk.Part> parts = new java.util.HashSet<>(List.of(new Toolbar()));
      Workbench desk = new Workbench(new Mdi(), List.of(new Offering("A/B", DOT, null)), List.of(), parts);
      JComponent north = (JComponent) desk.getComponent(0);
      assertEquals(1, north.getComponentCount());

      parts.clear();
      desk.changed();

      assertEquals(0, north.getComponentCount());
    });
  }
}
