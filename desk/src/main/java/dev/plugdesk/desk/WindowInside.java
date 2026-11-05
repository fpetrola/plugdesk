package dev.plugdesk.desk;

import dev.crystal.plugins.api.RoleInterface;

import javax.swing.JComponent;
import java.util.Map;

/**
 * What a window of the desk has inside that the desk does not see, by name, and the same back on the window opened
 * again. One for each kind of window, each only with the plugin that has it, and saying nothing of a window it does
 * not know.
 */
@RoleInterface
public interface WindowInside {
  void keep(JComponent window, Map<String, String> kept);

  void bringBack(JComponent window, Map<String, String> kept);
}
