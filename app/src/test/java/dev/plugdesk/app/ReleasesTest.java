package dev.plugdesk.app;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReleasesTest {

  @Test
  void theApplicationSaysWhatItIs() {
    Application sample = Application.it();

    assertEquals(".sample", sample.home(), "the home is named after the application unless it says otherwise");
    assertEquals(List.of("device-", "tool-"), sample.plugsIn());
    assertEquals("sample", sample.asking());
  }

  @Test
  void onlyTheReleasesOfSomethingToPlugInAreBoards() throws Exception {
    List<Releases.Board> boards = Releases.boards("""
        [ { "tag_name": "v1.0", "name": "The application",
            "assets": [ { "name": "app.jar", "browser_download_url": "https://x/app.jar", "id": 1, "size": 9 } ] },
          { "tag_name": "tool-notes-0.1", "name": "",
            "assets": [ { "name": "tool-notes-plugin-metadata.json", "browser_download_url": "https://x/m.json" },
                        { "name": "tool-notes-0.1.jar", "browser_download_url": "https://x/n.jar", "id": 2, "size": 5,
                          "digest": "sha256:abc" } ] } ]""");

    assertEquals(List.of(new Releases.Board("tool-notes-0.1", "tool-notes-0.1.jar", "https://x/n.jar", 2, 5, "abc",
        "https://x/m.json")), boards);
  }

  /** The same board at another version is the same board, which is what the file name hides. */
  @Test
  void aBoardIsTheSameBoardAtAnotherVersion() {
    assertEquals("tool-calls", Releases.whichBoard("tool-calls-0.0.2-alu-SNAPSHOT.jar"));
    assertEquals("tool-calls", Releases.whichBoard("tool-calls-0.0.3.jar"));
    assertEquals("device-mouse", Releases.whichBoard("device-mouse.jar"));
  }
}
