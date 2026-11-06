package dev.plugdesk.desk;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Windows docked at the sides of the desk, the way IDEs have them: one in the middle, and on the left, on the right and
 * below, each side a row of tabs. Every divider folds with a click; the views below can take the whole desk and give
 * it back. A docked window has no place of its own: what is kept is where the dividers are.
 */
public class Docked implements Layout {

  private final Map<Window.Side, JTabbedPane> sides = new EnumMap<>(Window.Side.class);
  private final JSplitPane middle;
  private final JSplitPane withRight;
  private final JSplitPane main;
  private int[] beforeMaximizing;

  public Docked() {
    for (Window.Side side : List.of(Window.Side.LEFT, Window.Side.CENTER, Window.Side.BOTTOM, Window.Side.RIGHT)) {
      JTabbedPane tabs = new JTabbedPane();
      tabs.setMinimumSize(new Dimension(0, 0));
      sides.put(side, tabs);
    }
    middle = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sides.get(Window.Side.CENTER), sides.get(Window.Side.BOTTOM));
    middle.setResizeWeight(0.85);
    withRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, middle, sides.get(Window.Side.RIGHT));
    withRight.setResizeWeight(1);
    main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sides.get(Window.Side.LEFT), withRight);
    main.setDividerLocation(260);
    for (JSplitPane split : splits()) {
      split.setOneTouchExpandable(true);
      split.setMinimumSize(new Dimension(0, 0));
    }
  }

  @Override
  public JComponent area() {
    return main;
  }

  @Override
  public void show(String title, JComponent window, Window.Side prefers) {
    JTabbedPane tabs = sides.get(prefers == Window.Side.FREE ? Window.Side.CENTER : prefers);
    if (tabs.indexOfComponent(window) < 0) {
      window.setMinimumSize(new Dimension(0, 0));
      tabs.addTab(title, window);
    }
    tabs.setSelectedComponent(window);
  }

  @Override
  public boolean shows(JComponent window) {
    return sideOf(window) != null;
  }

  /** The windows of each side, the sides in the order they are made, each side's tabs in order. */
  @Override
  public List<JComponent> backToFront() {
    List<JComponent> shown = new ArrayList<>();
    sides.values().forEach(tabs -> {
      for (int i = 0; i < tabs.getTabCount(); i++) shown.add((JComponent) tabs.getComponentAt(i));
    });
    return shown;
  }

  /** None: a docked window is where its side is. */
  @Override
  public Rectangle placeOf(JComponent window) {
    return null;
  }

  @Override
  public void place(JComponent window, Rectangle place) {
  }

  /** The side a window is docked at, or null when it is not on the desk. */
  public Window.Side sideOf(JComponent window) {
    return sides.entrySet().stream().filter(side -> side.getValue().indexOfComponent(window) >= 0)
        .map(Map.Entry::getKey).findFirst().orElse(null);
  }

  /** The views below take the whole desk; again, everything goes back where it was. */
  public void maximizeViews() {
    if (beforeMaximizing == null) {
      beforeMaximizing = dividers();
      put(main, 0);
      put(withRight, withRight.getWidth() - withRight.getDividerSize());
      put(middle, 0);
    } else {
      dividers(beforeMaximizing);
      beforeMaximizing = null;
    }
  }

  public boolean viewsMaximized() {
    return beforeMaximizing != null;
  }

  /** Maximize Views, under that menu. */
  public List<Action> actions(String menu) {
    return List.of(new Action() {
      public String path() {
        return menu + "/Maximize Views";
      }

      public List<KeyStroke> keys() {
        return List.of(KeyStroke.getKeyStroke("ctrl shift QUOTE"));
      }

      public void run() {
        maximizeViews();
      }
    });
  }

  /** Where the dividers are, to keep; as they were before maximizing when the views are maximized. */
  public Map<String, String> arrangement() {
    int[] at = beforeMaximizing != null ? beforeMaximizing : dividers();
    return Map.of(DIVIDERS, Arrays.toString(at).replaceAll("[\\[\\] ]", ""));
  }

  public void arrange(Map<String, String> kept) {
    if (kept.containsKey(DIVIDERS)) dividers(Arrays.stream(kept.get(DIVIDERS).split(",")).mapToInt(Integer::parseInt).toArray());
  }

  private static final String DIVIDERS = "dividers";

  /** Where each divider is drawn: one the layout put there itself says -1 when asked the usual way. */
  private int[] dividers() {
    return splits().stream().mapToInt(split -> split.getUI().getDividerLocation(split)).toArray();
  }

  /** From the outside in: a split inside another is measured on the size the outer one leaves it. */
  private void dividers(int[] at) {
    List<JSplitPane> splits = splits();
    for (int i = 0; i < splits.size(); i++) put(splits.get(i), at[i]);
  }

  /**
   * Puts a divider there once its split has taken the size its own split left it: one set before that is moved again
   * when the size arrives, by as much as the side its resize weight favours grows.
   */
  private static void put(JSplitPane split, int at) {
    split.doLayout();
    split.setDividerLocation(at);
    split.doLayout();
  }

  private List<JSplitPane> splits() {
    return List.of(main, withRight, middle);
  }
}
