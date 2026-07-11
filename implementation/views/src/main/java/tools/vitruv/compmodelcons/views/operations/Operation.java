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
    /**
     * Produce a list of object bindings, performing a step from origin to view.
     *
     * @param context the context of the operation
     * @return a list of object bindings
     */
    List<ObjectBinding> doGet(GetContext context);

    /**
     * Apply a change of the view towards the origin.
     *
     * @param change  the change of the view
     * @param target  the object binding of the changed object; must be a binding previously produced by this operation
     * @param context the context of the operation
     * @return the new object binding of the changed object
     */
    ObjectBinding doPut(EChange<EObject> change, ObjectBinding target, PutContext context);

    Optional<EChange<EObject>> doGetChange(EChange<EObject> change);
}
