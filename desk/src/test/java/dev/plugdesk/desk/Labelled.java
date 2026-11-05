package dev.plugdesk.desk;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.util.Map;

/** What a label says, kept the way a plugin keeps what its own window has inside. */
public class Labelled implements WindowInside {
  public void keep(JComponent window, Map<String, String> kept) {
    if (window instanceof JLabel label) kept.put("says", label.getText());
  }

  public void bringBack(JComponent window, Map<String, String> kept) {
    if (window instanceof JLabel label && kept.containsKey("says")) label.setText(kept.get("says"));
  }
}
