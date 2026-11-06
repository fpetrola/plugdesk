package dev.plugdesk.desk;

import dev.crystal.plugins.runtime.PluginService;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.MenuElement;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The desk: a menu bar, the windows, a status line, and whatever parts the plugins put beside them, all made of what is
 * offered. It names no menu and no window; an application is what it offers, the same way a plugin is.
 */
public class Workbench extends JPanel implements Desk {

  private final Layout layout;
  private final Collection<Action> actions;
  private final Collection<Actions> lists;
  private final Collection<Part> parts;
  private final Map<Part, JComponent> made = new java.util.LinkedHashMap<>();
  private final JPanel north = new JPanel(new java.awt.GridLayout(0, 1));
  private final JPanel south = new JPanel(new java.awt.GridLayout(0, 1));
  private final JMenuBar menus = new JMenuBar();
  private final JLabel status = new JLabel(" ");
  private static Workbench current;
  private final Map<String, JComponent> open = new HashMap<>();
  private static final String PLACE = "place";

  public Workbench(Layout layout, Collection<Action> actions, Collection<Actions> lists) {
    this(layout, actions, lists, List.of());
  }

  public Workbench(Layout layout, Collection<Action> actions, Collection<Actions> lists, Collection<Part> parts) {
    super(new BorderLayout());
    this.layout = layout;
    this.actions = actions;
    this.lists = lists;
    this.parts = parts;
    current = this;
    south.add(status);
    add(north, BorderLayout.NORTH);
    add(layout.area(), BorderLayout.CENTER);
    add(south, BorderLayout.SOUTH);
    changed();
  }

  /**
   * A desk of everything the plugins and the application offer, made again when they change. It offers the window
   * that brings plugins itself, under Tools: without it a desk with nothing on it would have no way to grow.
   */
  public static Workbench of(PluginService plugins, Layout layout, Actions... own) {
    List<Actions> mine = new java.util.ArrayList<>(List.of(own));
    mine.add(() -> List.of(new PluginsWindow("Tools/Plugins...", Widgets.icon(
        dev.crystal.plugins.swing.PluginIcons.class.getResource("icons/1F9E9.svg"), Widgets.TOOLBAR_ICON_SIZE))));
    Collection<Actions> theirs = plugins.roles(Actions.class);
    Workbench desk = new Workbench(layout, plugins.roles(Action.class), new java.util.AbstractCollection<>() {
      public java.util.Iterator<Actions> iterator() {
        return Stream.concat(mine.stream(), theirs.stream()).iterator();
      }

      public int size() {
        return mine.size() + theirs.size();
      }
    }, plugins.roles(Part.class));
    plugins.onChange(() -> SwingUtilities.invokeLater(desk::changed));
    return desk;
  }

  /** The desk on the screen, for a plugin's action that works on it: the last one made. */
  public static Workbench current() {
    return current;
  }

  /** How its windows are arranged. */
  public Layout arrangement() {
    return layout;
  }

  /** For the window that holds the desk. */
  public JMenuBar menus() {
    return menus;
  }

  @Override
  public void show(Window window) {
    JComponent shown = open.get(window.path());
    if (shown == null || !layout.shows(shown)) {
      shown = window.open();
      open.put(window.path(), shown);
    }
    layout.show(window.text(), shown, window.prefers());
  }

  @Override
  public String nameOf(JComponent window) {
    return open.entrySet().stream().filter(one -> one.getValue() == window).map(Map.Entry::getKey).findFirst().orElse(null);
  }

  @Override
  public JComponent window(String name) {
    return offered().stream().filter(one -> one instanceof Window && (one.path().equals(name) || one.text().equals(name))).findFirst()
        .map(one -> {
          show((Window) one);
          return open.get(one.path());
        }).orElse(null);
  }

  /** Every window on the desk, from the back to the front, with where it is and what it has inside. */
  public List<Map<String, String>> keep() {
    return layout.backToFront().stream().map(window -> {
      Map<String, String> kept = new java.util.TreeMap<>(inside(window));
      if (kept.isEmpty()) return kept;
      Rectangle place = layout.placeOf(window);
      if (place != null) kept.put(PLACE, place.x + "," + place.y + "," + place.width + "," + place.height);
      return kept;
    }).filter(kept -> !kept.isEmpty()).toList();
  }

  /** The windows kept, opened again in that order, each where it was. */
  public void bringBack(List<Map<String, String>> kept) {
    for (Map<String, String> one : kept) {
      JComponent window = bringBack(one);
      if (window == null || !one.containsKey(PLACE)) continue;
      int[] at = java.util.Arrays.stream(one.get(PLACE).split(",")).mapToInt(Integer::parseInt).toArray();
      layout.place(window, new Rectangle(at[0], at[1], at[2], at[3]));
    }
  }

  @Override
  public void say(String text) {
    status.setText(text);
  }

  @Override
  public void changed() {
    menus.removeAll();
    getInputMap(WHEN_IN_FOCUSED_WINDOW).clear();
    getActionMap().clear();
    groups.clear();
    List<Action> offered = offered();
    for (String name : dev.plugdesk.app.Application.it().menus()) menus.add(mnemonic(new JMenu(name)));
    offered.forEach(this::offer);
    for (java.awt.Component menu : menus.getComponents())
      if (((JMenu) menu).getItemCount() == 0) menus.remove(menu);
    eachMenu(menus.getSubElements(), menu -> Widgets.scrollingWhenLong(menu.getPopupMenu()));
    parts();
    menus.revalidate();
    revalidate();
    repaint();
  }

  /**
   * Everything offered, in the order it goes: by its place, and at the same place the application's lists before
   * what the plugins bring, each in the order it came.
   */
  public List<Action> offered() {
    return Stream.concat(lists.stream().flatMap(list -> list.actions().stream()), actions.stream())
        .sorted(Comparator.comparingInt(Action::place)).toList();
  }

  /** Does what was chosen: shows a window, runs anything else. */
  public void choose(Action action) {
    if (action instanceof Window window) show(window);
    else action.run();
  }

  /** The item of a menu that does it: ticked when it is on or off, grey when it needs a plugin. */
  public JMenuItem item(Action action) {
    JMenuItem item = action.ticked() == null ? new JMenuItem(doing(action)) : new JCheckBoxMenuItem(doing(action));
    if (action.ticked() != null) item.setSelected(action.ticked());
    if (action.needs() != null) {
      item.setForeground(java.awt.Color.GRAY);
      item.setToolTipText("Needs " + action.needs() + ": choosing it brings it");
    }
    if (!action.keys().isEmpty()) item.setAccelerator(action.keys().getFirst());
    return item;
  }

  /** A "+" button that lists what more could be had; choosing one brings its plugin. */
  public JButton more(List<Action> missing, String tooltip) {
    JButton more = new JButton("+");
    more.setToolTipText(tooltip);
    more.addActionListener(e -> {
      javax.swing.JPopupMenu everything = new javax.swing.JPopupMenu();
      missing.forEach(action -> everything.add(item(action)));
      Widgets.scrollingWhenLong(everything);
      everything.show(more, 0, more.getHeight());
    });
    return more;
  }

  private javax.swing.Action doing(Action action) {
    return new AbstractAction(action.needs() == null ? action.text() : action.text() + "  (+)", action.icon()) {
      @Override
      public void actionPerformed(ActionEvent event) {
        choose(action);
      }
    };
  }

  /** The button that does it, with its icon and its words as the tooltip. */
  public JButton button(Action action) {
    JButton button = new JButton(doing(action));
    button.setToolTipText(action.text());
    button.setHideActionText(true);
    return button;
  }

  private void offer(Action action) {
    JMenuItem item = item(action);
    into(menuOf(dev.plugdesk.app.Application.it().placed(action.path()), action.place()), item, action.place());
    action.keys().forEach(key -> getInputMap(WHEN_IN_FOCUSED_WINDOW).put(key, action.path()));
    getActionMap().put(action.path(), item.getAction());
  }

  /** Each part made once, on its side; told that things changed; gone when its plugin goes. */
  private void parts() {
    for (Part gone : List.copyOf(made.keySet()))
      if (!parts.contains(gone)) {
        north.remove(made.get(gone));
        south.remove(made.get(gone));
        made.remove(gone);
      }
    for (Part part : parts) {
      JComponent component = made.computeIfAbsent(part, one -> {
        JComponent made = one.make(this);
        if (BorderLayout.SOUTH.equals(one.side())) south.add(made, 0);
        else north.add(made);
        return made;
      });
      part.changed(this, component);
    }
  }

  /** The group of the last item put in each menu: a new group starts after a separator. */
  private final Map<JMenu, Integer> groups = new HashMap<>();

  /** A menu of the bar opens with Alt and its first letter. */
  private static JMenu mnemonic(JMenu menu) {
    menu.setMnemonic(menu.getText().charAt(0));
    return menu;
  }

  private static void eachMenu(MenuElement[] elements, java.util.function.Consumer<JMenu> doIt) {
    for (MenuElement element : elements) {
      if (!(element instanceof JMenu menu)) continue;
      eachMenu(menu.getPopupMenu().getSubElements(), doIt);
      doIt.accept(menu);
    }
  }

  /** Puts it at the end of that menu, after a separator when it starts a new hundred there. */
  private void into(JMenu menu, JMenuItem item, int place) {
    Integer group = groups.put(menu, place / 100);
    if (group != null && group != place / 100) menu.addSeparator();
    menu.add(item);
  }

  /** The menu an action goes in, made of its path the first time one asks for it, in its place. */
  private JMenu menuOf(String path, int place) {
    String[] names = path.split("/");
    MenuElement parent = menus;
    JMenu menu = null;
    for (int i = 0; i < names.length - 1; i++) {
      menu = child(parent, names[i]);
      if (menu == null) {
        menu = new JMenu(names[i]);
        if (parent instanceof JMenuBar bar) bar.add(mnemonic(menu));
        else into((JMenu) parent, menu, place);
      }
      parent = menu;
    }
    if (menu == null) throw new IllegalArgumentException("An action goes in a menu: " + path);
    return menu;
  }

  private static JMenu child(MenuElement parent, String name) {
    Stream<java.awt.Component> inside = parent instanceof JMenu menu
        ? Stream.of(menu.getMenuComponents()) : Stream.of(((JMenuBar) parent).getComponents());
    return inside.filter(c -> c instanceof JMenu m && m.getText().equals(name)).map(JMenu.class::cast)
        .findFirst().orElse(null);
  }


}
