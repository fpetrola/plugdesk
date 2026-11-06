package dev.plugdesk.desk;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WidgetsTest {

  @Test
  void anSvgIsDrawnAtTheSizeOfTheBar() {
    BufferedImage drawn = (BufferedImage) Widgets.loadIcon("square.svg").getImage();

    assertEquals(Widgets.TOOLBAR_ICON_SIZE, drawn.getWidth());
    assertEquals(0, drawn.getRGB(1, 1) >>> 24, "the corner is outside the square");
    assertEquals(0xffd22f27, drawn.getRGB(10, 10), "the middle is the square's red");
  }

  @Test
  void anIconThatIsNotThereSaysWhich() {
    assertEquals("no icon nowhere.svg", assertThrows(IllegalArgumentException.class, () -> Widgets.loadIcon("nowhere.svg")).getMessage());
  }
}
