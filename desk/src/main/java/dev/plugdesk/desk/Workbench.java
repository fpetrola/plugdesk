package dev.plugdesk.desk;

import dev.crystal.plugins.runtime.PluginService;

import javax.swing.AbstractAction;
import javax.swing.JButton;
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
  public void say(String text) {
    status.setText(text);
  }

  @Override
  public void changed() {
    menus.removeAll();
    buttons.removeAll();
    getInputMap(WHEN_IN_FOCUSED_WINDOW).clear();
    getActionMap().clear();
    offered().forEach(this::offer);
    menus.revalidate();
    buttons.revalidate();
    repaint();
  }

  private List<Action> offered() {
    return Stream.concat(actions.stream(), lists.stream().flatMap(list -> list.actions().stream()))
        .sorted(Comparator.comparingInt(Action::place).thenComparing(Action::path)).toList();
  }

  private void offer(Action action) {
    javax.swing.Action doing = new AbstractAction(action.text(), action.icon()) {
      @Override
      public void actionPerformed(ActionEvent event) {
        if (action instanceof Window window) show(window);
        else action.run();
      }
    };
    JMenuItem item = new JMenuItem(doing);
    if (!action.keys().isEmpty()) item.setAccelerator(action.keys().getFirst());
    menuOf(action.path()).add(item);
    action.keys().forEach(key -> getInputMap(WHEN_IN_FOCUSED_WINDOW).put(key, action.path()));
    getActionMap().put(action.path(), doing);
    if (action.icon() != null) {
      JButton button = buttons.add(doing);
      button.setToolTipText(action.text());
      button.setHideActionText(true);
    }
  }

  /** The menu an action goes in, made of its path the first time one asks for it. */
  private JMenu menuOf(String path) {
    String[] names = path.split("/");
    MenuElement parent = menus;
    JMenu menu = null;
    for (int i = 0; i < names.length - 1; i++) {
      menu = child(parent, names[i]);
      if (menu == null) {
        menu = new JMenu(names[i]);
        if (parent instanceof JMenuBar bar) bar.add(menu);
        else ((JMenu) parent).add(menu);
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
