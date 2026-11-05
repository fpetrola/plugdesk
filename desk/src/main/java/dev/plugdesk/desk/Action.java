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

  void run();

  default String text() {
    return path().substring(path().lastIndexOf('/') + 1);
  }

  /** The keys that do it; the first is the one a menu shows. */
  default List<KeyStroke> keys() {
    return List.of();
  }

  /** With one, it is also a button on the bar. */
  default Icon icon() {
    return null;
  }

  /** Lower goes first, in its menu and on the bar. */
  default int place() {
    return 100;
  }
}
