package dev.plugdesk.desk;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
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
 * it back. A docked window has no place of its own: what is kept is where the dividers are. A window that prefers no
 * side floats in a window of its own over the desk, as an IDE's dialogs do, and is not kept.
 */
public class Docked implements Layout {

  /** A side with one window shows that window alone, without a row of one tab: the looks that can, FlatLaf's, do. */
  static final String ONE_WINDOW_NO_TABS = "JTabbedPane.hideTabAreaWithOneTab";

  private final Map<Window.Side, JTabbedPane> sides = new EnumMap<>(Window.Side.class);
  private final JSplitPane middle;
  private final JSplitPane withRight;
  private final JSplitPane main;
  private int[] beforeMaximizing;
  /** The windows that float, each with what holds it on the screen: none where there is no screen. */
  private final Map<JComponent, java.util.Optional<javax.swing.JDialog>> floating = new java.util.LinkedHashMap<>();

  public Docked() {
    for (Window.Side side : List.of(Window.Side.LEFT, Window.Side.CENTER, Window.Side.BOTTOM, Window.Side.RIGHT)) {
      JTabbedPane tabs = new JTabbedPane();
      tabs.setMinimumSize(new Dimension(0, 0));
      tabs.putClientProperty(ONE_WINDOW_NO_TABS, true);
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
    if (prefers == Window.Side.FREE) {
      floating(title, window);
      return;
    }
    JTabbedPane tabs = sides.get(prefers);
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

  /**
   * In a window of its own over the desk's, the one already there brought to the front; closed, it is gone, and shown
   * again it is made again. An internal frame lends it its title, size and what it is made of, and is told when it is
   * activated, as on a desk of internal frames.
   */
  private void floating(String title, JComponent window) {
    java.util.Optional<javax.swing.JDialog> shown = floating.get(window);
    if (shown == null) {
      shown = java.awt.GraphicsEnvironment.isHeadless() ? java.util.Optional.empty() : java.util.Optional.of(dialog(title, window));
      floating.put(window, shown);
    }
    shown.ifPresent(dialog -> {
      dialog.setVisible(true);
      dialog.toFront();
    });
  }

  private javax.swing.JDialog dialog(String title, JComponent window) {
    javax.swing.JDialog dialog = new javax.swing.JDialog(javax.swing.SwingUtilities.getWindowAncestor(main), title,
        java.awt.Dialog.ModalityType.MODELESS);
    dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    if (window instanceof javax.swing.JInternalFrame frame) {
      dialog.setTitle(frame.getTitle());
      dialog.setContentPane(frame.getContentPane());
      dialog.setSize(frame.getSize());
      dialog.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowActivated(java.awt.event.WindowEvent event) {
          javax.swing.event.InternalFrameEvent activated =
              new javax.swing.event.InternalFrameEvent(frame, javax.swing.event.InternalFrameEvent.INTERNAL_FRAME_ACTIVATED);
          for (javax.swing.event.InternalFrameListener listener : frame.getInternalFrameListeners())
            listener.internalFrameActivated(activated);
        }
      });
    } else {
      dialog.add(window);
      dialog.pack();
    }
    dialog.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosed(java.awt.event.WindowEvent event) {
        floating.remove(window);
      }
    });
    dialog.setLocationRelativeTo(main);
    return dialog;
  }

  /** The side a window is docked at, FREE when it floats, or null when it is not on the desk. */
  public Window.Side sideOf(JComponent window) {
    if (floating.containsKey(window)) return Window.Side.FREE;
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

  /** Where the dividers are, to keep; as they were before maximizing when the views are maximized. */
  @Override
  public Map<String, String> arrangement() {
    int[] at = beforeMaximizing != null ? beforeMaximizing : dividers();
    return Map.of(DIVIDERS, Arrays.toString(at).replaceAll("[\\[\\] ]", ""));
  }

  @Override
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
