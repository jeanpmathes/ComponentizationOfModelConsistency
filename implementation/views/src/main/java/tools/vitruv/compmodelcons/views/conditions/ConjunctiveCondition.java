package tools.vitruv.compmodelcons.views.conditions;

import tools.vitruv.compmodelcons.views.bindings.OriginBinding;

public class ConjunctiveCondition implements Condition {
  private final Condition[] conditions;

  public ConjunctiveCondition(Condition... conditions) {
    this.conditions = conditions;
  }

  @Override
  public boolean evaluate(OriginBinding originBinding) {
    for (Condition condition : conditions) {
      if (!condition.evaluate(originBinding)) {
        return false;
      }
    }
    return true;
  }
}
