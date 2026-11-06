package dev.plugdesk.desk;

import dev.plugdesk.app.Configuration;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The window the desk lives in: where it was last time, or in the middle of the screen the first time; and closing it,
 * however it is closed, keeps where it is, lets the application keep what it keeps, and writes the settings down.
 */
public final class DeskWindow {
  private DeskWindow() {
  }

  public static JFrame open(String title, JComponent desk, JMenuBar menus, Runnable keeping) {
    JFrame frame = new JFrame(title);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setJMenuBar(menus);
    frame.setContentPane(desk);
    Rectangle place = DeskConfiguration.kept().place();
    if (place != null) frame.setBounds(place);
    else frame.setBounds(centred(1200, 800));
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        DeskConfiguration.kept().place(frame.getBounds());
        if (frame.getContentPane() instanceof Workbench workbench) DeskConfiguration.kept().keep(workbench.arrangement());
        keeping.run();
        Configuration.shared().save();
      }
    });
    frame.setVisible(true);
    if (desk instanceof Workbench workbench) arranged(frame, workbench);
    return frame;
  }

  /**
   * Puts a desk in a window that had something else, such as what it said while the desk was being made: with its
   * menus, and its layout arranged as it was kept.
   */
  public static void put(JFrame frame, Workbench desk) {
    frame.setJMenuBar(desk.menus());
    frame.setContentPane(desk);
    arranged(frame, desk);
  }

  /** Arranged once the window has given the desk its size: a divider put before that would move again. */
  private static void arranged(JFrame frame, Workbench desk) {
    frame.validate();
    DeskConfiguration.kept().arrange(desk.arrangement());
  }

  /**
   * An application whose desk is what its plugins bring: the plugins of its home started, a desk of what they and the
   * application offer, in its window. Everything else, menus included, arrives as plugins.
   */
  public static void start(String title, Layout layout, Actions... own) {
    SwingUtilities.invokeLater(() -> {
      Workbench desk = Workbench.of(dev.plugdesk.app.Plugins.managing(), layout, own);
      open(title, desk, desk.menus(), () -> { });
    });
  }

  /** Closes the window that has this in it, the same as its close button does. */
  public static void close(Component inside) {
    java.awt.Window window = SwingUtilities.getWindowAncestor(inside);
    if (window != null) window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
  }

  private static Rectangle centred(int width, int height) {
    Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
    return new Rectangle(screen.x + (screen.width - width) / 2, screen.y + (screen.height - height) / 2, width, height);
  }
}
