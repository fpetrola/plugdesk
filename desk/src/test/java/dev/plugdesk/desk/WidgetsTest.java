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

  /** A menu longer than the application says fits (menu-items; 18 when it says nothing) shows that many and two arrows. */
  @Test
  void aMenuScrollsOnlyWhenLongerThanWhatFits() {
    assertEquals(18, menuOf(18).getComponentCount(), "all of it");
    javax.swing.JPopupMenu scrolling = menuOf(19);
    assertEquals(20, scrolling.getComponentCount(), "18 and the arrows");
    assertEquals("\u25B2", ((javax.swing.JMenuItem) scrolling.getComponent(0)).getText());
  }

  private static javax.swing.JPopupMenu menuOf(int items) {
    javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
    for (int i = 0; i < items; i++) menu.add(new javax.swing.JMenuItem("item " + i));
    Widgets.scrollingWhenLong(menu);
    return menu;
  }
}
