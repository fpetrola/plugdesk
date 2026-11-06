package dev.plugdesk.standard.toolbar;

import dev.plugdesk.desk.Action;
import dev.plugdesk.desk.Part;
import dev.plugdesk.desk.Widgets;
import dev.plugdesk.desk.Workbench;

import javax.swing.JComponent;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.util.List;

/** A bar of buttons above the windows: one for everything offered with an icon, and a "+" for what more can be had. */
public class Toolbar implements Part {

  @Override
  public String side() {
    return BorderLayout.NORTH;
  }

  @Override
  public JComponent make(Workbench desk) {
    JToolBar bar = new JToolBar();
    bar.setFloatable(false);
    return bar;
  }

  @Override
  public void changed(Workbench desk, JComponent made) {
    made.removeAll();
    List<Action> offered = desk.offered();
    offered.stream().filter(action -> action.icon() != null && action.needs() == null).forEach(action -> made.add(desk.button(action)));
    List<Action> missing = offered.stream().filter(action -> action.needs() != null).toList();
    if (!missing.isEmpty()) made.add(desk.more(missing, "More that can be had: choosing one brings its plugin"));
    Widgets.tighten(made, 3);
    made.revalidate();
    made.repaint();
  }
}
