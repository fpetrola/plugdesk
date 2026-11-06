package dev.plugdesk.standard.docks;

import dev.plugdesk.desk.Action;
import dev.plugdesk.desk.Actions;
import dev.plugdesk.desk.Docked;
import dev.plugdesk.desk.Workbench;

import javax.swing.KeyStroke;
import java.util.List;

/** What a desk that docks its windows can do with them: the views below take the whole desk, and give it back. */
public class DockMenu implements Actions {

  @Override
  public List<Action> actions() {
    Workbench desk = Workbench.current();
    if (desk == null || !(desk.arrangement() instanceof Docked docked)) return List.of();
    return List.of(new Action() {
      public String path() {
        return "Window/Maximize Views";
      }

      public List<KeyStroke> keys() {
        return List.of(KeyStroke.getKeyStroke("ctrl shift QUOTE"));
      }

      public void run() {
        docked.maximizeViews();
      }
    });
  }
}
