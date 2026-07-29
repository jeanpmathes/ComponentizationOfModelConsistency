package tools.vitruv.compmodelcons.views.expressions;

import tools.vitruv.compmodelcons.views.bindings.OriginBinding;

@FunctionalInterface
public interface Condition {
    boolean evaluate(OriginBinding originBinding);
}
