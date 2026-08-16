package tools.vitruv.compmodelcons.views.operations;

import java.util.List;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.OriginBinding;

/**
 * An origin operation.
 * Origin operations are all operations that occur before view-side elements exist.
 */
public interface OriginOperation {
  List<OriginBinding> doGet(GetContext context);

  OriginBinding doPut(EChange<EObject> viewChange, OriginBinding target, PutContext context);
}
