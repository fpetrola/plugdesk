package dev.plugdesk.desk;

import dev.plugdesk.app.Configuration;
import dev.plugdesk.app.Section;

import java.awt.Rectangle;
import java.util.Map;
import java.util.TreeMap;

/**
 * What the desk keeps of itself from one run to the next: where its window was, the look it wore, and how its layout
 * was arranged (where the dividers were, in a docked one).
 */
@Section("desk")
public class DeskConfiguration {
  public String look;
  public int x = -1;
  public int y = -1;
  public int width;
  public int height;
  public Map<String, String> arrangement = new TreeMap<>();

  public static DeskConfiguration kept() {
    return Configuration.shared().of(DeskConfiguration.class);
  }

  /** Where the window was, or null the first time. */
  public Rectangle place() {
    return width > 0 && height > 0 ? new Rectangle(x, y, width, height) : null;
  }

  /** Keeps how that layout is arranged now. */
  public void keep(Layout layout) {
    arrangement = new TreeMap<>(layout.arrangement());
  }

  /** That layout arranged as it was kept; as it comes, the first time. */
  public void arrange(Layout layout) {
    if (!arrangement.isEmpty()) layout.arrange(arrangement);
  }

  public void place(Rectangle place) {
    x = place.x;
    y = place.y;
    width = place.width;
    height = place.height;
  }
}
