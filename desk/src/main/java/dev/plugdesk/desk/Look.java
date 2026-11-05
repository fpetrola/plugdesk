package dev.plugdesk.desk;

import dev.crystal.plugins.api.RoleInterface;

import java.util.List;

/**
 * A family of looks the desk can wear: what it is called, which it has, and how each is put on. Those that come with
 * Java the desk wears itself; those of other libraries come in a plugin that carries the library.
 */
@RoleInterface
public interface Look {
  String family();

  List<String> names();

  /** Puts it on Swing; whatever is painted afterwards comes from it. */
  void wear(String name) throws Exception;
}
