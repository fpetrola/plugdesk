package dev.plugdesk.desk;

import dev.crystal.plugins.swing.PluginsPanel;
import dev.plugdesk.app.Plugins;

import javax.swing.Icon;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * What the environment is made of, shown by crystal's panel: what each jar brought, what there is of each kind, and
 * what can still be had. A jar copied into the folder by hand is plugged in when somebody looks.
 */
public final class PluginsWindow implements Window {
  private final String path;
  private final Icon icon;

  public PluginsWindow(String path, Icon icon) {
    this.path = path;
    this.icon = icon;
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public Icon icon() {
    return icon;
  }

  @Override
  public JInternalFrame open() {
    JInternalFrame window = new JInternalFrame("Plugins", true, true, true, true);
    window.add(new PluginsPanel(Plugins.managing()));
    window.setBounds(60, 60, 700, 460);
    window.addInternalFrameListener(new InternalFrameAdapter() {
      public void internalFrameActivated(InternalFrameEvent shown) {
        Plugins.readWhatArrived();
      }
    });
    return window;
  }
}
