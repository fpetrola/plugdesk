package dev.plugdesk.desk;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Free windows inside the desk, each in a frame of its own. */
public class Mdi implements Layout {

  private final JDesktopPane desktop = new JDesktopPane();
  private final Map<JComponent, JInternalFrame> frames = new IdentityHashMap<>();

  @Override
  public JComponent area() {
    return desktop;
  }

  @Override
  public void show(String title, JComponent window, Window.Side prefers) {
    JInternalFrame frame = frames.computeIfAbsent(window, inside -> framed(title, inside));
    if (frame.getParent() == null) desktop.add(frame);
    frame.setVisible(true);
    frame.toFront();
    try {
      frame.setSelected(true);
    } catch (PropertyVetoException refused) {
      // shown either way
    }
  }

  @Override
  public boolean shows(JComponent window) {
    JInternalFrame frame = frames.get(window);
    return frame != null && !frame.isClosed();
  }

  @Override
  public List<JComponent> backToFront() {
    List<JComponent> shown = new ArrayList<>();
    for (JInternalFrame frame : desktop.getAllFrames())
      frames.forEach((window, its) -> {
        if (its == frame) shown.addFirst(window);
      });
    return shown;
  }

  @Override
  public Rectangle placeOf(JComponent window) {
    return frames.get(window).getNormalBounds();
  }

  @Override
  public void place(JComponent window, Rectangle place) {
    frames.get(window).setBounds(place);
  }

  /** Closing the window in front, closing every one, and putting them in cascade or side by side, under that menu. */
  public List<Action> actions(String menu) {
    return List.of(new Doing(menu + "/Close Active Window", "ctrl W", 100, this::closeSelected),
        new Doing(menu + "/Close All Windows", "ctrl shift W", 101, this::closeAll),
        new Doing(menu + "/Cascade", "alt 1", 200, this::cascade),
        new Doing(menu + "/Tile", "alt 2", 201, this::tile));
  }

  private record Doing(String path, String key, int place, Runnable doing) implements Action {
    public void run() {
      doing.run();
    }

    public List<javax.swing.KeyStroke> keys() {
      return List.of(javax.swing.KeyStroke.getKeyStroke(key));
    }
  }

  private void closeSelected() {
    JInternalFrame frame = desktop.getSelectedFrame();
    if (frame != null) close(frame);
  }

  private void closeAll() {
    for (JInternalFrame frame : desktop.getAllFrames()) close(frame);
  }

  private static void close(JInternalFrame frame) {
    try {
      frame.setClosed(true);
    } catch (PropertyVetoException refused) {
      // it stays open: it said so
    }
  }

  private void cascade() {
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
  private void tile() {
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

  private static JInternalFrame framed(String title, JComponent inside) {
    if (inside instanceof JInternalFrame frame) return frame;
    JInternalFrame frame = new JInternalFrame(title, true, true, true, true);
    frame.add(inside);
    frame.pack();
    return frame;
  }
}
