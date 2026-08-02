package tools.vitruv.compmodelcons.views.conditions;

import tools.vitruv.compmodelcons.views.bindings.OriginBinding;

@FunctionalInterface
public interface Condition {
    boolean evaluate(OriginBinding originBinding);

    Condition TRUE = originBinding -> true;
}
