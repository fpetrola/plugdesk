package dev.plugdesk.desk;

import dev.crystal.plugins.api.Offer;
import dev.crystal.plugins.api.PluginArtifact;
import dev.plugdesk.app.Plugins;

import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * What the plugins that are not here offer, each where the application says that kind of thing goes, and all of them
 * again under one menu. Choosing one brings its plugin and then does what was chosen. Asked of the catalogue away from
 * the window's thread: until it answers there is nothing.
 */
public final class Offered implements Actions {

  /** Where each role goes, the first that the offering extension is; and the menu that has them all, or null. */
  private final Map<Class<?>, String> where;
  private final String everything;
  private final Consumer<Offer> done;
  private final Supplier<Map<PluginArtifact, List<Offer>>> available;
  private final Consumer<String> bring;
  private Map<PluginArtifact, List<Offer>> known = Map.of();

  /** @param done what to do once the plugin is in: the thing that was chosen, which only the application knows how */
  public Offered(Map<Class<?>, String> where, String everything, Consumer<Offer> done) {
    this(where, everything, done, () -> Plugins.areRead() ? Plugins.managing().availableOffering() : Map.of(), Plugins::add);
  }

  Offered(Map<Class<?>, String> where, String everything, Consumer<Offer> done,
          Supplier<Map<PluginArtifact, List<Offer>>> available, Consumer<String> bring) {
    this.where = where;
    this.everything = everything;
    this.done = done;
    this.available = available;
    this.bring = bring;
  }

  /** Asks the catalogue again, and once it answers runs {@code arrived} on the window's thread. */
  public void look(Runnable arrived) {
    new SwingWorker<Map<PluginArtifact, List<Offer>>, Void>() {
      protected Map<PluginArtifact, List<Offer>> doInBackground() {
        return available.get();
      }

      protected void done() {
        try {
          known = get();
        } catch (Exception cannotAsk) {
          return;
        }
        arrived.run();
      }
    }.execute();
  }

  /** What the plugins not here would bring of that role, each under its own name: for a place of its own. */
  public List<Action> of(Class<?> role) {
    List<Action> holes = new ArrayList<>();
    known.forEach((artifact, offers) -> offers.stream().filter(offer -> offer.is(role))
        .forEach(offer -> holes.add(new Hole(offer.text(), artifact, offer))));
    return holes;
  }

  @Override
  public List<Action> actions() {
    List<Action> holes = new ArrayList<>();
    known.forEach((artifact, offers) -> offers.forEach(offer -> {
      where.entrySet().stream().filter(role -> offer.is(role.getKey())).findFirst()
          .ifPresent(role -> holes.add(new Hole(role.getValue() + "/" + offer.text(), artifact, offer)));
      if (everything != null) holes.add(new Hole(everything + "/" + offer.text(), artifact, offer));
    }));
    return holes;
  }

  private final class Hole implements Action {
    private final String path;
    private final PluginArtifact artifact;
    private final Offer offer;

    Hole(String path, PluginArtifact artifact, Offer offer) {
      this.path = path;
      this.artifact = artifact;
      this.offer = offer;
    }

    public String path() {
      return path;
    }

    public String needs() {
      return artifact.id();
    }

    public int place() {
      return 900;
    }

    public void run() {
      new SwingWorker<Void, Void>() {
        protected Void doInBackground() {
          bring.accept(artifact.id());
          return null;
        }

        protected void done() {
          done.accept(offer);
        }
      }.execute();
    }
  }
}
