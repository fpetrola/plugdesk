package dev.plugdesk.desk;

import javax.swing.JComponent;

/** How the windows of a desk are arranged. */
public interface Layout {

  /** What goes in the middle of the desk. */
  JComponent area();

  /** Puts it on the screen, in front. */
  void show(String title, JComponent window, Window.Side prefers);

  /** Whether it is still on the screen. */
  boolean shows(JComponent window);
}
