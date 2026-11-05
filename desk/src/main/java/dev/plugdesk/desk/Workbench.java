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
import javax.swing.JToolBar;
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
 * The desk: a menu bar, a bar of buttons, the windows and a status line, all made of what is offered. It names no
 * menu and no window; an application is what it offers, the same way a plugin is.
 */
public class Workbench extends JPanel implements Desk {

  private final Layout layout;
  private final Collection<Action> actions;
  private final Collection<Actions> lists;
  private final JMenuBar menus = new JMenuBar();
  final JToolBar buttons = new JToolBar();
  private final JLabel status = new JLabel(" ");
  private final Map<String, JComponent> open = new HashMap<>();
  private static final String PLACE = "place";

  public Workbench(Layout layout, Collection<Action> actions, Collection<Actions> lists) {
    super(new BorderLayout());
    this.layout = layout;
    this.actions = actions;
    this.lists = lists;
    buttons.setFloatable(false);
    add(buttons, BorderLayout.NORTH);
    add(layout.area(), BorderLayout.CENTER);
    add(status, BorderLayout.SOUTH);
    changed();
  }

  /** A desk of everything the plugins and the application offer, made again when they change. */
  public static Workbench of(PluginService plugins, Layout layout) {
    Workbench desk = new Workbench(layout, plugins.roles(Action.class), plugins.roles(Actions.class));
    plugins.onChange(() -> SwingUtilities.invokeLater(desk::changed));
    return desk;
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
      kept.put(PLACE, place.x + "," + place.y + "," + place.width + "," + place.height);
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
    buttons.removeAll();
    getInputMap(WHEN_IN_FOCUSED_WINDOW).clear();
    getActionMap().clear();
    groups.clear();
    List<Action> offered = offered();
    for (String name : dev.plugdesk.app.Application.it().menus()) menus.add(mnemonic(new JMenu(name)));
    offered.forEach(this::offer);
    for (java.awt.Component menu : menus.getComponents())
      if (((JMenu) menu).getItemCount() == 0) menus.remove(menu);
    List<Action> missing = offered.stream().filter(action -> action.needs() != null).toList();
    if (!missing.isEmpty()) buttons.add(more(missing, "More that can be had: choosing one brings its plugin"));
    tighten(buttons, 3);
    eachMenu(menus.getSubElements(), menu -> scrollingWhenLong(menu.getPopupMenu()));
    menus.revalidate();
    buttons.revalidate();
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
      scrollingWhenLong(everything);
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

  private void offer(Action action) {
    JMenuItem item = item(action);
    javax.swing.Action doing = item.getAction();
    into(menuOf(action.path(), action.place()), item, action.place());
    action.keys().forEach(key -> getInputMap(WHEN_IN_FOCUSED_WINDOW).put(key, action.path()));
    getActionMap().put(action.path(), doing);
    if (action.icon() != null && action.needs() == null) {
      JButton button = buttons.add(doing);
      button.setToolTipText(action.text());
      button.setHideActionText(true);
    }
  }

  /** The group of the last item put in each menu: a new group starts after a separator. */
  private final Map<JMenu, Integer> groups = new HashMap<>();

  /**
   * Trims the margin a button keeps round its icon, and keeps it from taking the keyboard: a button that holds the
   * focus is one that Enter presses, and Enter belongs to whatever is being typed into.
   */
  public static void tighten(java.awt.Container toolBar, int sideMargin) {
    for (java.awt.Component component : toolBar.getComponents()) {
      if (component instanceof javax.swing.AbstractButton button) {
        button.setMargin(new java.awt.Insets(2, sideMargin, 2, sideMargin));
        button.setFocusPainted(false);
        // And it does not take the keyboard: a button that holds the focus is a button that Enter
        // presses, which is how Enter stopped reaching the machine and pressed the turbo instead.
        button.setFocusable(false);
      }
    }
  }

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

  /** Cuantos items muestra un menu largo a la vez. */
  private static final int ITEMS_AT_ONCE = 18;

  /**
   * Un menu con mas de lo que entra deja de llenar la pantalla: muestra unos cuantos items y dos
   * flechas que lo recorren al dejarles el mouse encima, y la rueda tambien. Los items siguen siendo
   * del menu: puestos en un panel con barra, Swing no los reconocia como suyos y cerraba el menu en
   * cuanto el mouse pasaba por uno. Llamar despues de llenarlo.
   */
  public static void scrollingWhenLong(javax.swing.JPopupMenu menu) {
    scrollingWhenLong(menu, 0);
  }

  /** Lo mismo, dejando fijos arriba los primeros: lo que tiene que estar siempre a mano. */
  public static void scrollingWhenLong(javax.swing.JPopupMenu menu, int fixedOnTop) {
    java.awt.Component[] fixed = java.util.Arrays.copyOf(menu.getComponents(), fixedOnTop);
    java.awt.Component[] all = java.util.Arrays.copyOfRange(menu.getComponents(), fixedOnTop, menu.getComponentCount());
    if (all.length <= ITEMS_AT_ONCE) return;
    int[] first = {0};
    javax.swing.JMenuItem up = new javax.swing.JMenuItem("\u25B2");
    javax.swing.JMenuItem down = new javax.swing.JMenuItem("\u25BC");
    // Del ancho del mas ancho de todos, visible o no: si no, cambiaba al recorrerlo.
    java.awt.Insets border = menu.getInsets();
    int widest = java.util.Arrays.stream(menu.getComponents()).mapToInt(item -> item.getPreferredSize().width).max().orElse(0)
        + border.left + border.right;
    Runnable show = () -> {
      menu.removeAll();
      for (java.awt.Component kept : fixed) menu.add(kept);
      menu.add(up);
      for (int i = first[0]; i < Math.min(all.length, first[0] + ITEMS_AT_ONCE); i++) menu.add(all[i]);
      menu.add(down);
      up.setEnabled(first[0] > 0);
      down.setEnabled(first[0] + ITEMS_AT_ONCE < all.length);
      menu.setPreferredSize(null);
      menu.setPreferredSize(new java.awt.Dimension(widest, menu.getPreferredSize().height));
      menu.pack();
      menu.revalidate();
      menu.repaint();
    };
    java.util.function.IntConsumer move = step -> {
      int moved = Math.max(0, Math.min(all.length - ITEMS_AT_ONCE, first[0] + step));
      if (moved != first[0]) {
        first[0] = moved;
        show.run();
      }
    };
    for (javax.swing.JMenuItem arrow : java.util.List.of(up, down)) {
      arrow.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
      int step = arrow == up ? -1 : 1;
      javax.swing.Timer going = new javax.swing.Timer(90, e -> move.accept(step));
      arrow.getModel().addChangeListener(e -> {
        if (arrow.isArmed() && arrow.isEnabled()) going.start();
        else going.stop();
      });
      // Una flecha no es una eleccion: tocarla no cierra el menu.
      arrow.setUI(new javax.swing.plaf.basic.BasicMenuItemUI() {
        @Override
        protected void doClick(javax.swing.MenuSelectionManager manager) {
        }

        /** Centrada en si misma: el lugar que Swing le da depende de los items de al lado, que cambian. */
        @Override
        public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
          if (arrow.isArmed() && arrow.isEnabled()) {
            g.setColor(javax.swing.UIManager.getColor("MenuItem.selectionBackground"));
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
          }
          g.setColor(arrow.isEnabled() ? arrow.getForeground() : javax.swing.UIManager.getColor("MenuItem.disabledForeground"));
          g.setFont(arrow.getFont());
          java.awt.FontMetrics metrics = g.getFontMetrics();
          g.drawString(arrow.getText(), (c.getWidth() - metrics.stringWidth(arrow.getText())) / 2,
              (c.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        }
      });
    }
    menu.addMouseWheelListener(wheel -> move.accept(wheel.getWheelRotation()));
    show.run();
  }
}
