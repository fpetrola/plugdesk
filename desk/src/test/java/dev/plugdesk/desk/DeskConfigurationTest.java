package dev.plugdesk.desk;

import dev.plugdesk.app.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Rectangle;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeskConfigurationTest {

  @Test
  void whereTheWindowWasAndItsLookComeBack(@TempDir Path home) {
    File file = home.resolve("config.json").toFile();
    Configuration first = new Configuration(file);
    assertNull(first.of(DeskConfiguration.class).place(), "nowhere the first time");
    first.of(DeskConfiguration.class).place(new Rectangle(10, 20, 900, 700));
    first.of(DeskConfiguration.class).look = "System / Nimbus";
    first.save();

    DeskConfiguration again = new Configuration(file).of(DeskConfiguration.class);

    assertEquals(new Rectangle(10, 20, 900, 700), again.place());
    assertEquals("System / Nimbus", again.look);
  }

  /** Where a docked desk's dividers were comes back the next time; a home kept before there was an arrangement opens as it was. */
  @Test
  void howTheLayoutWasArrangedComesBack(@TempDir Path home) throws Exception {
    File file = home.resolve("config.json").toFile();
    Files.writeString(file.toPath(), "{\"desk\":{\"look\":\"Darcula\",\"x\":1,\"y\":2,\"width\":800,\"height\":600}}");
    Configuration old = new Configuration(file);
    assertEquals(new Rectangle(1, 2, 800, 600), old.of(DeskConfiguration.class).place());
    assertTrue(old.of(DeskConfiguration.class).arrangement.isEmpty(), "nothing arranged yet");
    javax.swing.SwingUtilities.invokeAndWait(() -> {
      Docked docked = sized(new Docked());
      docked.arrange(Map.of("dividers", "200,700,500"));
      old.of(DeskConfiguration.class).keep(docked);
    });
    old.save();

    DeskConfiguration again = new Configuration(file).of(DeskConfiguration.class);
    javax.swing.SwingUtilities.invokeAndWait(() -> {
      Docked docked = sized(new Docked());
      again.arrange(docked);

      assertEquals(Map.of("dividers", "200,700,500"), docked.arrangement());
    });
    assertEquals("Darcula", again.look);
  }

  private static Docked sized(Docked docked) {
    Workbench desk = new Workbench(docked, List.of(), List.of());
    desk.setSize(1000, 800);
    layOut(desk);
    return docked;
  }

  private static void layOut(java.awt.Container container) {
    container.doLayout();
    for (java.awt.Component part : container.getComponents())
      if (part instanceof java.awt.Container inside) layOut(inside);
  }
}
