package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.Context;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.List;
import java.util.Optional;

/**
 * A part of a bidirectional transformation, itself a bidirectional transformation as well.
 */
public interface Operation {
    List<ObjectBinding> get(Context context);

    Optional<ObjectBinding> put(EChange<EObject> eChange, ObjectBinding target, Context context);

    Optional<EChange<EObject>> getChange(EChange<EObject> change);
}
