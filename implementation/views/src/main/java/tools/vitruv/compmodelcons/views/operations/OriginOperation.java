package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.List;

public interface OriginOperation {
    List<ObjectBinding> doGet(GetContext context);

    ObjectBinding doPut(EChange<EObject> viewChange, ObjectBinding target, PutContext context);

    List<ObjectBinding> doUpdatingGet(List<ObjectBinding> previous, EChange<EObject> originChange, GetContext context);
}
