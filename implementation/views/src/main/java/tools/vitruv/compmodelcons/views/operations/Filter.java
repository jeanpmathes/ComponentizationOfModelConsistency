package tools.vitruv.compmodelcons.views.operations;

import java.util.List;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.OriginBinding;
import tools.vitruv.compmodelcons.views.conditions.Condition;

public class Filter implements OriginOperation {
  private final OriginOperation origin;
  private final Condition filter;

  public Filter(Condition filter, OriginOperation origin) {
    this.origin = origin;
    this.filter = filter;
  }

  @Override
  public List<OriginBinding> doGet(GetContext context) {
    return origin
        .doGet(context)
        .stream()
        .filter(filter::evaluate)
        .toList();
  }

  @Override
  public OriginBinding doPut(EChange<EObject> viewChange, OriginBinding target,
                             PutContext context) {
    if (viewChange instanceof DeleteEObject<EObject> deleteEObject && !filter.evaluate(target)) {
      context
          .getCorrespondences()
          .removeCorrespondence(target.originObjects(), deleteEObject.getAffectedElement());
      return OriginBinding.empty();
    }

    return origin.doPut(viewChange, target, context);
  }

  @Override
  public List<OriginBinding> doUpdatingGet(List<OriginBinding> previous,
                                           EChange<EObject> originChange, GetContext context) {
    return origin
        .doUpdatingGet(previous, originChange, context)
        .stream()
        .filter(filter::evaluate)
        .toList();
  }
}
