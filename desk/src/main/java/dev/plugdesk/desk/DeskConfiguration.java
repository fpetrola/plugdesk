package dev.plugdesk.desk;

import dev.plugdesk.app.Configuration;
import dev.plugdesk.app.Section;

import java.awt.Rectangle;

/** What the desk keeps of itself from one run to the next: where its window was and the look it wore. */
@Section("desk")
public class DeskConfiguration {
  public String look;
  public int x = -1;
  public int y = -1;
  public int width;
  public int height;

  public static DeskConfiguration kept() {
    return Configuration.shared().of(DeskConfiguration.class);
  }

  /** Where the window was, or null the first time. */
  public Rectangle place() {
    return width > 0 && height > 0 ? new Rectangle(x, y, width, height) : null;
  }

  public void place(Rectangle place) {
    x = place.x;
    y = place.y;
    width = place.width;
    height = place.height;
  }
}
