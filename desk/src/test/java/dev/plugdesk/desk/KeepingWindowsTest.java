package dev.plugdesk.desk;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeepingWindowsTest {

  record Labelling(String path) implements Window {
    public JComponent open() {
      return new JLabel("new");
    }
  }

  private static final List<Action> OFFERED = List.of(new Labelling("Window/Notes"), new Labelling("Window/Log"));

  @Test
  void theDeskComesBackAsItWasLeft() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      Mdi mdi = new Mdi();
      Workbench desk = new Workbench(mdi, OFFERED, List.of());
      ((JLabel) desk.window("Window/Notes")).setText("written");
      mdi.place(desk.window("Window/Notes"), new Rectangle(10, 20, 300, 200));
      mdi.place(desk.window("Window/Log"), new Rectangle(50, 60, 400, 100));

      List<Map<String, String>> kept = desk.keep();
      Mdi again = new Mdi();
      Workbench another = new Workbench(again, OFFERED, List.of());
      another.bringBack(kept);

      assertEquals(List.of(
          Map.of(Desk.NAME, "Window/Notes", "says", "written", "place", "10,20,300,200"),
          Map.of(Desk.NAME, "Window/Log", "says", "new", "place", "50,60,400,100")), kept, "from the back to the front");
      assertEquals(kept, another.keep(), "and the same again on the desk it came back on");
    });
  }

  @Test
  void aWindowTheDeskDidNotOpenIsNotKept() {
    Workbench desk = new Workbench(new Mdi(), OFFERED, List.of());

    assertEquals(Map.of(), desk.inside(new JLabel("loose")));
  }
}
