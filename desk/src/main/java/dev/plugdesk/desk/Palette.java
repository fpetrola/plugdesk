package dev.plugdesk.desk;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Everything the desk offers, found by typing part of its name: what is here is done, and what needs a plugin brings it
 * and then is done. It is how a person finds out there is more than they see.
 */
public final class Palette extends JPanel {

  private record Row(Action action) {
    @Override
    public String toString() {
      return action.needs() == null ? action.text() : action.text() + "   \u2014   needs " + action.needs();
    }
  }

  private final Workbench desk;
  private final Runnable close;
  private final List<Row> all;
  private final DefaultListModel<Row> shown = new DefaultListModel<>();
  private final JList<Row> list = new JList<>(shown);
  private final JTextField asked = new JTextField();

  /** The one action that opens it, under that path and with those keys. */
  public static Action opening(Workbench desk, String path, String keys) {
    return new Action() {
      public String path() {
        return path;
      }

      public List<javax.swing.KeyStroke> keys() {
        return List.of(javax.swing.KeyStroke.getKeyStroke(keys));
      }

      public void run() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(desk), "What do you want to do?", ModalityType.MODELESS);
        dialog.setContentPane(new Palette(desk, dialog::dispose));
        dialog.setSize(560, 380);
        dialog.setLocationRelativeTo(desk);
        dialog.setVisible(true);
      }
    };
  }

  Palette(Workbench desk, Runnable close) {
    super(new BorderLayout(4, 4));
    this.desk = desk;
    this.close = close;
    this.all = desk.offered().stream().map(Row::new).toList();
    add(asked, BorderLayout.NORTH);
    add(new JScrollPane(list), BorderLayout.CENTER);
    list.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
    asked.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e) { filter(); }
      public void removeUpdate(DocumentEvent e) { filter(); }
      public void changedUpdate(DocumentEvent e) { filter(); }
    });
    KeyAdapter keys = new KeyAdapter() {
      public void keyPressed(KeyEvent key) {
        if (key.getKeyCode() == KeyEvent.VK_DOWN && key.getSource() == asked) list.requestFocusInWindow();
        if (key.getKeyCode() == KeyEvent.VK_ENTER) chosen(list.getSelectedValue());
        if (key.getKeyCode() == KeyEvent.VK_ESCAPE) close.run();
      }
    };
    asked.addKeyListener(keys);
    list.addKeyListener(keys);
    list.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent click) {
        if (click.getClickCount() == 2) chosen(list.getSelectedValue());
      }
    });
    filter();
  }

  /** What the rows say now, for whoever wants to see what typing left. */
  List<String> shown() {
    return java.util.Collections.list(shown.elements()).stream().map(Row::toString).toList();
  }

  void type(String words) {
    asked.setText(words);
  }

  private void filter() {
    String words = asked.getText().trim().toLowerCase();
    shown.clear();
    all.stream().filter(row -> row.toString().toLowerCase().contains(words)).forEach(shown::addElement);
    if (!shown.isEmpty()) list.setSelectedIndex(0);
  }

  private void chosen(Row row) {
    if (row == null) return;
    close.run();
    desk.choose(row.action());
  }
}
