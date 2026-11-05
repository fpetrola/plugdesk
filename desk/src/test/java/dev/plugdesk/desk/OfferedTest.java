package dev.plugdesk.desk;

import dev.crystal.plugins.api.Offer;
import dev.crystal.plugins.api.PluginArtifact;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfferedTest {

  interface Board {
  }

  interface Gadget {
  }

  private static final PluginArtifact MOUSE = new PluginArtifact("device-mouse", "1.0", "0".repeat(64));
  private static final Offer POINTING = new Offer("Mouse", "Kempston Mouse", "", List.of(Board.class.getName()));
  private static final Offer CLOCK = new Offer("Clock", "A clock", "", List.of(Gadget.class.getName()));

  @Test
  void eachOfferGoesWhereItsKindGoesAndAllUnderOneMenu() throws Exception {
    Map<Class<?>, String> where = new LinkedHashMap<>(Map.of(Board.class, "Emulator/Equipment"));
    Offered offered = new Offered(where, "Discover", offer -> { }, () -> Map.of(MOUSE, List.of(POINTING, CLOCK)), id -> { });
    arrived(offered);

    assertEquals(List.of("Emulator/Equipment/Kempston Mouse needs device-mouse", "Discover/Kempston Mouse needs device-mouse",
        "Discover/A clock needs device-mouse"),
        offered.actions().stream().map(action -> action.path() + " needs " + action.needs()).toList());
  }

  @Test
  void choosingOneBringsItsPluginAndThenDoesIt() throws Exception {
    List<String> happened = new ArrayList<>();
    CountDownLatch finished = new CountDownLatch(1);
    Offered offered = new Offered(Map.of(), "Discover", offer -> {
      happened.add("done " + offer.text());
      finished.countDown();
    }, () -> Map.of(MOUSE, List.of(POINTING)), id -> happened.add("brought " + id));
    arrived(offered);

    offered.actions().getFirst().run();

    assertTrue(finished.await(5, TimeUnit.SECONDS));
    assertEquals(List.of("brought device-mouse", "done Kempston Mouse"), happened);
  }

  private static void arrived(Offered offered) throws Exception {
    CountDownLatch answered = new CountDownLatch(1);
    SwingUtilities.invokeAndWait(() -> offered.look(answered::countDown));
    assertTrue(answered.await(5, TimeUnit.SECONDS));
  }
}
