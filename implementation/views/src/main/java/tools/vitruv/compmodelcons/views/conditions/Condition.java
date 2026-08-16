package tools.vitruv.compmodelcons.views.conditions;

import tools.vitruv.compmodelcons.views.bindings.OriginBinding;

@FunctionalInterface
public interface Condition {
  Condition TRUE = originBinding -> true;
  boolean evaluate(OriginBinding originBinding);
}
