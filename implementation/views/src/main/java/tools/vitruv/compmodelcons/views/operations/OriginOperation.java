package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.OriginBinding;

import java.util.List;

public interface OriginOperation {
    List<OriginBinding> doGet(GetContext context);

    OriginBinding doPut(EChange<EObject> viewChange, OriginBinding target, PutContext context);

    List<OriginBinding> doUpdatingGet(List<OriginBinding> previous, EChange<EObject> originChange, GetContext context);
}
