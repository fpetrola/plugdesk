package dev.plugdesk.standard.suggestions;

import dev.plugdesk.desk.Action;
import dev.plugdesk.desk.Actions;
import dev.plugdesk.desk.Offered;
import dev.plugdesk.desk.Workbench;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What can be done, found by typing part of its name, and under Discover what the plugins that are not here would
 * bring: choosing one brings its plugin and then does it.
 */
public class Suggestions implements Actions {

  private final Offered offered = new Offered(Map.of(), "Discover", offer -> {
    Workbench desk = Workbench.current();
    desk.changed();
    desk.offered().stream().filter(action -> action.getClass().getName().equals(offer.extension())).findFirst()
        .ifPresent(desk::choose);
  });
  private boolean asked;

  @Override
  public List<Action> actions() {
    Workbench desk = Workbench.current();
    if (desk == null) return List.of();
    if (!asked) {
      asked = true;
      offered.look(desk::changed);
    }
    List<Action> all = new ArrayList<>(List.of(Palette.opening(desk, "Discover/What do you want to do?...", "ctrl shift P")));
    all.addAll(offered.actions());
    return all;
  }
}
