package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.List;
import java.util.Optional;

/**
 * A part of a bidirectional transformation, itself a bidirectional transformation as well.
 */
public interface Operation {
    List<ObjectBinding> get(GetContext context);

    ObjectBinding put(EChange<EObject> change, ObjectBinding target, PutContext context);

    Optional<EChange<EObject>> getChange(EChange<EObject> change);
}
