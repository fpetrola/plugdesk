package dev.plugdesk.desk;

/** What a window or an action can ask of the desk it is on. */
public interface Desk {

  /** Shows it in front: the one already open, or a new one. */
  void show(Window window);

  /** A line on the status bar. */
  void say(String text);

  /** What is offered changed: menus and buttons are made again. */
  void changed();
}
