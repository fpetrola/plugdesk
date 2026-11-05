package dev.plugdesk.desk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.UIManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LookAndFeelsTest {

  @BeforeEach
  void nothingWornYet() {
    Painted.worn.clear();
  }

  @Test
  void everyLookIsAnActionUnderItsFamily() {
    List<String> paths = LookAndFeels.actions("Window/Look", chosen -> { }).actions().stream().map(Action::path).toList();

    assertEquals(List.of("Window/Look/Painted/One", "Window/Look/Painted/Two", "Window/Look/System/Metal",
        "Window/Look/System/Nimbus", "Window/Look/System/Native"), paths);
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
