package dev.plugdesk.standard.docks;

import dev.plugdesk.desk.Action;
import dev.plugdesk.desk.Docked;
import dev.plugdesk.desk.Mdi;
import dev.plugdesk.desk.Workbench;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockMenuTest {

  @Test
  void aDeskThatDocksItsWindowsCanMaximizeItsViews() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Docked docked = new Docked();
      new Workbench(docked, List.of(), List.of());
      List<Action> actions = new DockMenu().actions();

      assertEquals(List.of("Window/Maximize Views"), actions.stream().map(Action::path).toList());
      actions.getFirst().run();
      assertTrue(docked.viewsMaximized());
    });
  }

  @Test
  void aDeskOfFreeWindowsHasNothingToMaximize() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      new Workbench(new Mdi(), List.of(), List.of());

      assertEquals(List.of(), new DockMenu().actions());
    });
  }
}
