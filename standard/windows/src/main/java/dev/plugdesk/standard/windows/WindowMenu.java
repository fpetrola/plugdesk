package dev.plugdesk.standard.windows;

import dev.plugdesk.desk.Action;
import dev.plugdesk.desk.Actions;
import dev.plugdesk.desk.Workbench;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import java.beans.PropertyVetoException;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Window menu of a desk whose windows are free inside it: closing the one in front or every one, and putting
 * them in cascade or side by side. A desk that docks its windows has none of this.
 */
public class WindowMenu implements Actions {

  @Override
  public List<Action> actions() {
    Workbench desk = Workbench.current();
    if (desk == null || !(desk.arrangement().area() instanceof JDesktopPane desktop)) return List.of();
    return List.of(new Doing("Window/Close Active Window", "ctrl W", 100, desktop, WindowMenu::closeSelected),
        new Doing("Window/Close All Windows", "ctrl shift W", 101, desktop, WindowMenu::closeAll),
        new Doing("Window/Cascade", "alt 1", 200, desktop, WindowMenu::cascade),
        new Doing("Window/Tile", "alt 2", 201, desktop, WindowMenu::tile));
  }

  private record Doing(String path, String key, int place, JDesktopPane desktop, Consumer<JDesktopPane> doing) implements Action {
    public void run() {
      doing.accept(desktop);
    }

    public List<javax.swing.KeyStroke> keys() {
      return List.of(javax.swing.KeyStroke.getKeyStroke(key));
    }
  }


  private static void closeSelected(JDesktopPane desktop) {
    JInternalFrame frame = desktop.getSelectedFrame();
    if (frame != null) close(frame);
  }

  private static void closeAll(JDesktopPane desktop) {
    for (JInternalFrame frame : desktop.getAllFrames()) close(frame);
  }

  private static void close(JInternalFrame frame) {
    try {
      frame.setClosed(true);
    } catch (PropertyVetoException refused) {
      // it stays open: it said so
    }
  }

  private static void cascade(JDesktopPane desktop) {
    int x = 0;
    int y = 0;
    int width = desktop.getWidth() / 2;
    int height = desktop.getHeight() / 2;
    for (JInternalFrame frame : desktop.getAllFrames()) {
      if (frame.isIcon()) continue;
      normal(frame).reshape(x, y, width, height);
      x += 30;
      y += 30;
      if (x + width > desktop.getWidth()) x = 0;
      if (y + height > desktop.getHeight()) y = 0;
    }
  }

  /** Side by side in a grid as square as the count allows, the last rows taking one more each. */
  private static void tile(JDesktopPane desktop) {
    JInternalFrame[] frames = desktop.getAllFrames();
    if (frames.length == 0) return;
    int rows = (int) Math.sqrt(frames.length);
    int cols = frames.length / rows;
    int extra = frames.length % rows;
    int w = desktop.getWidth() / cols;
    int h = desktop.getHeight() / rows;
    int y = 0;
    for (int i = 0, index = 0; i < rows; i++, y += h) {
      for (int j = 0, x = 0; j < cols && index < frames.length; j++, index++, x += w)
        if (!frames[index].isIcon()) normal(frames[index]).reshape(x, y, w, h);
      if (i == rows - extra - 1) w = desktop.getWidth() / ++cols;
    }
  }

  private static JInternalFrame normal(JInternalFrame frame) {
    try {
      frame.setMaximum(false);
    } catch (PropertyVetoException refused) {
      // reshaped all the same
    }
    return frame;
  }
}
