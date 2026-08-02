package tools.vitruv.compmodelcons.views.conditions;

import tools.vitruv.compmodelcons.views.bindings.OriginBinding;

import java.util.Arrays;

public class ConjunctiveCondition implements Condition {
    private final Condition[] conditions;

    public ConjunctiveCondition(Condition... conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean evaluate(OriginBinding originBinding) {
        return Arrays.stream(conditions).allMatch(condition -> condition.evaluate(originBinding));
    }
}
