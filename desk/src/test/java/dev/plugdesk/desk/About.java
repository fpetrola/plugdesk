package dev.plugdesk.desk;

/** What an application offers of its own, found through its META-INF/services like a plugin's extension. */
public class About implements Action {
  public String path() {
    return "Help/About";
  }

  public void run() {
  }
}
