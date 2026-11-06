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
        keeping.run();
        Configuration.shared().save();
      }
    });
    frame.setVisible(true);
    return frame;
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
