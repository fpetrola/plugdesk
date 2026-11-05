package dev.plugdesk.desk;

import dev.crystal.plugins.api.RoleInterface;

import java.util.List;

/** Actions that come and go on their own, such as the files opened last: asked again whenever the desk changes. */
@RoleInterface
public interface Actions {
  List<Action> actions();
}
