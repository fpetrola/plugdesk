package dev.plugdesk.standard.suggestions;

import dev.plugdesk.app.Application;
import dev.plugdesk.desk.Action;
import dev.plugdesk.desk.Actions;
import dev.plugdesk.desk.Offered;
import dev.plugdesk.desk.Workbench;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What can be done, found by typing part of its name or of what it does, and under Discover what the plugins that are
 * not here would bring: choosing one brings its plugin and then does it. The application may say where the palette goes
 * and its keys ({@code palette} and {@code palette.keys} in its file), or that it has one of its own ({@code palette=}
 * with nothing: then only Discover).
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
    List<Action> all = new ArrayList<>();
    String path = Application.it().said("palette");
    if (path == null) path = "Discover/What do you want to do?...";
    String keys = Application.it().said("palette.keys");
    if (!path.isBlank()) all.add(Palette.opening(desk, path, keys == null ? "ctrl shift P" : keys));
    all.addAll(offered.actions());
    return all;
  }
}
