package tools.vitruv.compmodelcons.views.conditions;

import java.util.Arrays;
import tools.vitruv.compmodelcons.views.bindings.OriginBinding;

public class ConjunctiveCondition implements Condition {
  private final Condition[] conditions;

  public ConjunctiveCondition(Condition... conditions) {
    this.conditions = conditions;
  }

  @Override
  public boolean evaluate(OriginBinding originBinding) {
    return Arrays
        .stream(conditions)
        .allMatch(condition -> condition.evaluate(originBinding));
  }
}
