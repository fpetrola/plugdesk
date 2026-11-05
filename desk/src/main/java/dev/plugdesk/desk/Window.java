package dev.plugdesk.desk;

import javax.swing.JComponent;

/** A window of the desk. Choosing it shows it, the one already open if there is one. */
public interface Window extends Action {

  enum Side { FREE, CENTER, LEFT, RIGHT, BOTTOM }

  JComponent open();

  /** Where it would rather be; a layout that has no sides puts it where it can. */
  default Side prefers() {
    return Side.FREE;
  }

  /** Shown by the desk it is on, never run by itself. */
  @Override
  default void run() {
  }
}
