package dev.plugdesk.desk;

import dev.crystal.plugins.api.RoleInterface;

import javax.swing.JComponent;

/**
 * Something a plugin puts on the desk beside the windows, such as a bar of buttons: made once, on the side it says,
 * and told whenever what is offered changes.
 */
@RoleInterface
public interface Part {

  /**
   * {@link java.awt.BorderLayout#NORTH}, a row under the menus; {@link java.awt.BorderLayout#LINE_END}, at the right end
   * of the first of those rows, beside what is there, as an IDE's toolbar has its run configurations; or
   * {@link java.awt.BorderLayout#SOUTH}.
   */
  String side();

  JComponent make(Workbench desk);

  /** What is offered changed: menus were made again, and this part may want to follow. */
  default void changed(Workbench desk, JComponent made) {
  }
}
