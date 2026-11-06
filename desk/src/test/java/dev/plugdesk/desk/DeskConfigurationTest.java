package dev.plugdesk.desk;

import dev.plugdesk.app.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Rectangle;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
