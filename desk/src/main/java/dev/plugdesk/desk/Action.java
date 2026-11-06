package dev.plugdesk.desk;

import dev.crystal.plugins.api.RoleInterface;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.util.List;

/**
 * Something a person can do, wherever the desk offers it: an item in a menu, its keys, and a button when it has an
 * icon. The desk has no menus of its own: they are the paths of what is offered.
 */
@RoleInterface
public interface Action {

  /** Where it goes in the menus, each menu followed by a slash, and its own name last: {@code File/Open...}. */
  String path();

  /** A name as one step of a path: a slash in it would read as two, so it becomes the slash that is not one. */
  static String step(String name) {
    return name.replace('/', '\u2215');
  }

  void run();

  default String text() {
    return path().substring(path().lastIndexOf('/') + 1);
  }

  /** What it does, in a few words, for whoever looks for it by what it does; null when its name says it. */
  default String description() {
    return null;
  }

  /** The keys that do it; the first is the one a menu shows. */
  default List<KeyStroke> keys() {
    return List.of();
  }

  /** With one, it is also a button on the bar. */
  default Icon icon() {
    return null;
  }

  /** Lower goes first, in its menu and on the bar; a separator goes between hundreds. */
  default int place() {
    return 100;
  }

  /** The plugin that has to be brought before this can be done, or null when it can be done now. */
  default String needs() {
    return null;
  }

  /** Whether it is on, for something that is either on or off; null for anything else. */
  default Boolean ticked() {
    return null;
  }
}
