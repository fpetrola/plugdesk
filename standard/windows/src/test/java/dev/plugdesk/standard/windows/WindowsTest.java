package dev.plugdesk.standard.windows;

import dev.plugdesk.desk.Action;
import dev.plugdesk.desk.Docked;
import dev.plugdesk.desk.Mdi;
import dev.plugdesk.desk.Window;
import dev.plugdesk.desk.Workbench;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowsTest {

  @Test
  void theWindowMenuClosesCascadesAndTiles() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Mdi mdi = new Mdi();
      new Workbench(mdi, List.of(), List.of());
      mdi.area().setSize(800, 600);
      JLabel one = new JLabel("one");
      JLabel two = new JLabel("two");
      mdi.show("One", one, Window.Side.FREE);
      mdi.show("Two", two, Window.Side.FREE);
      List<Action> actions = new WindowMenu().actions();

      assertEquals(List.of("Window/Close Active Window", "Window/Close All Windows", "Window/Cascade", "Window/Tile"),
          actions.stream().map(Action::path).toList());
      actions.get(3).run();
      assertEquals(600, mdi.placeOf(one).height, "side by side, each the whole height");
      assertEquals(java.util.Set.of(0, 400), java.util.Set.of(mdi.placeOf(one).x, mdi.placeOf(two).x), "one beside the other");
      actions.get(2).run();
      assertEquals(30, Math.abs(mdi.placeOf(one).x - mdi.placeOf(two).x), "in cascade, thirty apart");
      actions.get(1).run();
      assertTrue(mdi.backToFront().isEmpty(), "and all closed");
    });
  }

  @Test
  void aDeskThatDocksItsWindowsHasNoWindowMenu() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      new Workbench(new Docked(), List.of(), List.of());

      assertEquals(List.of(), new WindowMenu().actions());
    });
  }
}
