package dev.plugdesk.desk;

import dev.plugdesk.app.Plugins;

import javax.swing.JComponent;
import java.util.Map;
import java.util.TreeMap;

/** What a window or an action can ask of the desk it is on. */
public interface Desk {

  /** The key a kept window is named under: the name the desk opened it under. */
  String NAME = "kind";

  /** Shows it in front: the one already open, or a new one. */
  void show(Window window);

  /** A line on the status bar. */
  void say(String text);

  /** What is offered changed: menus and buttons are made again. */
  void changed();

  /** The name the desk opened that window under, or null when it is not one the desk opened. */
  String nameOf(JComponent window);

  /** The window of that name, opened as choosing it opens it; null when nothing offers it. */
  JComponent window(String name);

  /**
   * That window as it can be kept: its name and what each {@link WindowInside} says it has inside. Empty for a window
   * the desk did not open, which there is no way to open again.
   */
  default Map<String, String> inside(JComponent window) {
    String name = nameOf(window);
    if (name == null) return Map.of();
    Map<String, String> kept = new TreeMap<>(Map.of(NAME, name));
    Plugins.found(WindowInside.class).forEach(inside -> inside.keep(window, kept));
    return kept;
  }

  /** The window kept, open again by its name and given back what it had inside; null when nothing opens it now. */
  default JComponent bringBack(Map<String, String> kept) {
    JComponent window = kept.containsKey(NAME) ? window(kept.get(NAME)) : null;
    if (window != null) Plugins.found(WindowInside.class).forEach(inside -> inside.bringBack(window, kept));
    return window;
  }
}
