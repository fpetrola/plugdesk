package dev.plugdesk.standard.looks;

import dev.plugdesk.desk.*;

import java.util.ArrayList;
import java.util.List;

/** A family of looks found the way a plugin's would be, which puts Metal on and remembers what it was asked. */
public class Painted implements Look {
  static final List<String> worn = new ArrayList<>();

  public String family() {
    return "Painted";
  }

  public List<String> names() {
    return List.of("One", "Two");
  }

  public void wear(String name) throws Exception {
    worn.add(name);
    javax.swing.UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
  }
}
