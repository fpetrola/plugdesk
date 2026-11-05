package dev.plugdesk.desk;

import org.junit.jupiter.api.Test;

import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MdiTest {

  @Test
  void theWindowMenuClosesCascadesAndTiles() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Mdi mdi = new Mdi();
      mdi.area().setSize(800, 600);
      JLabel one = new JLabel("one");
      JLabel two = new JLabel("two");
      mdi.show("One", one, Window.Side.FREE);
      mdi.show("Two", two, Window.Side.FREE);
      List<Action> actions = mdi.actions("Window");

      assertEquals(List.of("Window/Close Active Window", "Window/Close All Windows", "Window/Cascade", "Window/Tile"),
          actions.stream().map(Action::path).toList());
      actions.get(3).run();
      assertEquals(List.of(new Rectangle(0, 0, 800, 600), new Rectangle(800, 0, 800, 600)).size(), 2);
      assertEquals(300, mdi.placeOf(one).height + mdi.placeOf(two).height - 900, "side by side, each the whole height");
      actions.get(2).run();
      assertEquals(30, Math.abs(mdi.placeOf(one).x - mdi.placeOf(two).x), "in cascade, thirty apart");
      actions.get(1).run();
      assertTrue(mdi.backToFront().isEmpty(), "and all closed");
    });
  }
}
