package dev.plugdesk.desk;

import javax.swing.JComponent;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

/** How the windows of a desk are arranged. */
public interface Layout {

  /** What goes in the middle of the desk. */
  JComponent area();

  /** Puts it on the screen, in front. */
  void show(String title, JComponent window, Window.Side prefers);

  /** Whether it is still on the screen. */
  boolean shows(JComponent window);

  /** What is on the screen, from the back to the front. */
  List<JComponent> backToFront();

  /** Where it is, as it would be neither maximised nor minimised; null where windows have no place of their own. */
  Rectangle placeOf(JComponent window);

  void place(JComponent window, Rectangle place);

  /** How it is arranged, to keep from one run to the next: nothing for a layout where each window has its place. */
  default Map<String, String> arrangement() {
    return Map.of();
  }

  /** Arranged again as it was kept. */
  default void arrange(Map<String, String> kept) {
  }
}
