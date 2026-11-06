package dev.plugdesk.desk;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockedTest {

  record View(String path, Window.Side prefers) implements Window {
    public JComponent open() {
      return new JLabel(path);
    }
  }

  private static final List<Action> IDE = List.of(new View("View/Project", Window.Side.LEFT), new View("View/Editor", Window.Side.CENTER),
      new View("View/Problems", Window.Side.BOTTOM), new View("View/Output", Window.Side.BOTTOM),
      new View("View/Plans", Window.Side.RIGHT), new View("View/Loose", Window.Side.FREE));

  @Test
  void eachWindowGoesToItsSideAndTwoBelowAreTwoTabs() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Docked docked = new Docked();
      Workbench desk = new Workbench(docked, IDE, List.of());
      IDE.forEach(view -> desk.window(view.path()));

      assertEquals(List.of(Window.Side.LEFT, Window.Side.CENTER, Window.Side.BOTTOM, Window.Side.BOTTOM, Window.Side.RIGHT,
          Window.Side.CENTER), IDE.stream().map(view -> docked.sideOf(desk.window(view.path()))).toList(),
          "and a free window goes in the middle");
      JComponent problems = desk.window("View/Problems");
      assertSame(problems, desk.window("View/Problems"), "shown again, the same tab");
      assertEquals(6, docked.backToFront().size());
    });
  }

  @Test
  void theViewsBelowTakeTheWholeDeskAndGiveItBack() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Docked docked = new Docked();
      Workbench desk = new Workbench(docked, IDE, List.of());
      IDE.forEach(view -> desk.window(view.path()));
      desk.setSize(1000, 800);
      layOut(desk);
      docked.arrange(Map.of("dividers", "200,700,500"));
      layOut(desk);

      docked.maximizeViews();
      JSplitPane main = (JSplitPane) docked.area();
      assertTrue(docked.viewsMaximized());
      List<Integer> at = where(main);
      assertTrue(at.get(0) <= 2, "the left side folded, against its border: " + at);
      assertTrue(at.get(1) > 1000 - 260 - 20, "the right side folded: " + at);
      assertTrue(at.get(2) <= 2, "the middle folded, against its border: " + at);
      assertEquals(Map.of("dividers", "200,700,500"), docked.arrangement(), "kept as it was before");

      docked.maximizeViews();
      assertFalse(docked.viewsMaximized());
      assertEquals(Map.of("dividers", "200,700,500"), docked.arrangement());
    });
  }

  @Test
  void aDockedWindowIsKeptWithoutAPlace() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Workbench desk = new Workbench(new Docked(), IDE, List.of());
      desk.window("View/Problems");

      assertEquals(List.of(Map.of(Desk.NAME, "View/Problems", "says", "View/Problems")), desk.keep());
    });
  }

  @Test
  void dividersTheLayoutPutWhereTheyAreComeBackFromMaximizing() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Docked docked = new Docked();
      Workbench desk = new Workbench(docked, IDE, List.of());
      IDE.forEach(view -> desk.window(view.path()));
      desk.setSize(1000, 800);
      layOut(desk);
      List<Integer> before = where(docked.area());

      docked.maximizeViews();
      layOut(desk);
      desk.window("View/Plans").setPreferredSize(new java.awt.Dimension(600, 100));
      docked.maximizeViews();
      layOut(desk);

      assertEquals(before, where(docked.area()), "where each divider is, as it was, though the plans ask for more room now");
    });
  }

  /** Where every divider under that one is drawn, outermost first. */
  private static List<Integer> where(java.awt.Component component) {
    List<Integer> at = new java.util.ArrayList<>();
    if (component instanceof JSplitPane split) at.add(split.getUI().getDividerLocation(split));
    if (component instanceof java.awt.Container container)
      for (java.awt.Component part : container.getComponents()) at.addAll(where(part));
    return at;
  }

  private static void layOut(java.awt.Container container) {
    container.doLayout();
    for (java.awt.Component part : container.getComponents())
      if (part instanceof java.awt.Container inside) layOut(inside);
  }
}
