package dev.plugdesk.desk;

import dev.crystal.plugins.runtime.PluginService;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkbenchTest {

  private static final Icon DOT = new javax.swing.ImageIcon(new java.awt.image.BufferedImage(8, 8, 2));

  record Doing(String path, Icon icon, int place, List<KeyStroke> keys, AtomicInteger runs) implements Action {
    Doing(String path) {
      this(path, null, 100, List.of(), new AtomicInteger());
    }

    public void run() {
      runs.incrementAndGet();
    }
  }

  @Test
  void theMenusAreThePathsOfWhatIsOffered() throws Exception {
    onTheEventThread(() -> {
      Workbench desk = new Workbench(new Mdi(), List.of(new Doing("A/D", DOT, 100, List.of(), new AtomicInteger()),
          new Doing("A/B/C"), new Doing("Z/First", null, 1, List.of(), new AtomicInteger())), List.of());

      assertEquals(List.of("Z: First", "A: D", "A: B: C"), tree(desk.menus()), "at the same place, in the order offered");
    });
  }

  @Test
  void everyKeyOfAnActionDoesIt() throws Exception {
    onTheEventThread(() -> {
      KeyStroke first = KeyStroke.getKeyStroke("ctrl O");
      KeyStroke second = KeyStroke.getKeyStroke("F3");
      Doing open = new Doing("File/Open...", null, 100, List.of(first, second), new AtomicInteger());
      Workbench desk = new Workbench(new Mdi(), List.of(open), List.of());

      Object bound = desk.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(second);
      desk.getActionMap().get(bound).actionPerformed(null);

      assertEquals(1, open.runs().get());
      assertEquals(first, desk.menus().getMenu(0).getItem(0).getAccelerator());
    });
  }

  @Test
  void whatComesAndGoesIsAskedAgainWhenTheDeskChanges() throws Exception {
    onTheEventThread(() -> {
      List<Action> recent = new ArrayList<>(List.of(new Doing("File/Recent/one")));
      Workbench desk = new Workbench(new Mdi(), List.of(), List.of(() -> recent));
      recent.add(new Doing("File/Recent/two"));

      desk.changed();

      assertEquals(List.of("File: Recent: one", "File: Recent: two"), tree(desk.menus()));
    });
  }

  @Test
  void aWindowIsShownOnceAndOpenedAgainWhenClosed() throws Exception {
    onTheEventThread(() -> {
      AtomicInteger opened = new AtomicInteger();
      Window notes = new Window() {
        public String path() {
          return "Window/Notes";
        }

        public JComponent open() {
          opened.incrementAndGet();
          return new JLabel("notes");
        }
      };
      Window other = new Window() {
        public String path() {
          return "Window/Other";
        }

        public JComponent open() {
          return new JLabel("other");
        }
      };
      Mdi mdi = new Mdi();
      Workbench desk = new Workbench(mdi, List.of(notes, other), List.of());

      desk.menus().getMenu(0).getItem(0).doClick();
      JInternalFrame first = (JInternalFrame) mdi.area().getComponent(0);
      desk.show(other);
      desk.show(notes);

      assertEquals(1, opened.get());
      assertSame(first, mdi.area().getComponent(0));
      assertEquals("Notes", first.getTitle());

      first.dispose();
      desk.show(notes);

      assertEquals(2, opened.get());
      assertNotSame(first, mdi.area().getComponent(0));
    });
  }

  @Test
  void theStatusLineSaysWhatItIsTold() throws Exception {
    onTheEventThread(() -> {
      Workbench desk = new Workbench(new Mdi(), List.of(), List.of());

      desk.say("indexing 40%");

      assertEquals("indexing 40%", ((JLabel) ((JComponent) desk.getComponent(2)).getComponent(0)).getText());
    });
  }

  @Test
  void aDeskOfPluginsOffersTheApplicationsOwnAndTheWindowThatBringsPlugins() throws Exception {
    try (PluginService plugins = PluginService.builder().build()) {
      plugins.start();
      onTheEventThread(() -> {
        Workbench desk = Workbench.of(plugins, new Mdi(), () -> List.of(new Doing("File/Open...")));

        assertEquals(List.of("File: Open...", "Help: About", "Tools: Plugins..."), tree(desk.menus()),
            "its own, the application's found as a plugin's would be, and the plugins window under Tools");
      });
    }
  }

  /** Each item of the bar with the menus it is in, outermost first. */
  private static List<String> tree(JMenuBar bar) {
    List<String> items = new ArrayList<>();
    for (Component menu : bar.getComponents()) collect((JMenu) menu, ((JMenu) menu).getText(), items);
    return items;
  }

  private static void collect(JMenu menu, String path, List<String> items) {
    for (Component part : menu.getMenuComponents()) {
      if (part instanceof JMenu inner) collect(inner, path + ": " + inner.getText(), items);
      else if (part instanceof JMenuItem item) items.add(path + ": " + item.getText());
    }
  }

  private static void onTheEventThread(Runnable steps) throws Exception {
    Throwable[] failed = new Throwable[1];
    SwingUtilities.invokeAndWait(() -> {
      try {
        steps.run();
      } catch (Throwable failure) {
        failed[0] = failure;
      }
    });
    if (failed[0] instanceof Error error) throw error;
    if (failed[0] != null) throw new AssertionError(failed[0]);
  }
}
