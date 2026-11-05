package dev.plugdesk.desk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JMenu;
import javax.swing.UIManager;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LookAndFeelsTest {

  @BeforeEach
  void nothingWornYet() {
    Painted.worn.clear();
  }

  @Test
  void theMenuHasEveryFamilyAPluginBringsAndThoseOfJava() {
    JMenu menu = new JMenu("Look");

    LookAndFeels.fillMenu(menu, chosen -> { });

    assertEquals(List.of("Painted: One, Two", "System: Metal, Nimbus, Native"), Stream.of(menu.getMenuComponents())
        .map(family -> ((JMenu) family).getText() + ": " + String.join(", ", Stream.of(((JMenu) family).getMenuComponents())
            .map(item -> ((javax.swing.JMenuItem) item).getText()).toList())).toList());
  }

  @Test
  void aLookIsWornByTheNameItWasSavedUnder() {
    LookAndFeels.install("Painted / Two");

    assertEquals(List.of("Two"), Painted.worn);
  }

  @Test
  void choosingAFamilyWearsItsFirstLook() {
    LookAndFeels.wear(new Painted());

    assertEquals(List.of("One"), Painted.worn);
  }

  @Test
  void aLookThatIsGoneGivesWayToTheApplications() throws Exception {
    UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");

    LookAndFeels.install("Gone / Away");

    assertEquals(List.of(), Painted.worn);
    assertEquals("Metal", UIManager.getLookAndFeel().getName(), "an application that says nothing starts in Metal");
  }

}
