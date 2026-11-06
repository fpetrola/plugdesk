package dev.plugdesk.standard.looks;

import dev.crystal.plugins.api.HasLifecycle;
import dev.plugdesk.desk.Action;
import dev.plugdesk.desk.Actions;

import javax.swing.SwingUtilities;
import java.util.List;

/**
 * Looks and feels: every one Java has and every family a plugin brings, under Window, the one chosen kept for next
 * time and worn as soon as this plugin starts.
 */
public class Looks implements Actions, HasLifecycle {

  @Override
  public void onStart() {
    SwingUtilities.invokeLater(LookAndFeels::wearTheKept);
  }

  /** Asked whenever what is plugged in changes: a look that went with its plugin comes back with it. */
  @Override
  public List<Action> actions() {
    LookAndFeels.lookPluginsChanged();
    return LookAndFeels.actions("Window/Look&Feel").actions();
  }
}
