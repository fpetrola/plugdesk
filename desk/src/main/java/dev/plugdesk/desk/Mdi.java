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

  private static JInternalFrame framed(String title, JComponent inside) {
    if (inside instanceof JInternalFrame frame) return frame;
    JInternalFrame frame = new JInternalFrame(title, true, true, true, true);
    frame.add(inside);
    frame.pack();
    return frame;
  }
}
