package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueUpdateBinding;

import java.util.Optional;

/**
 * A part of a bidirectional transformation, itself a bidirectional transformation as well.
 * These operations are specific for features.
 */
public interface FeatureOperation {
    /**
     * Produce a feature binding, performing a step from origin to view.
     *
     * @param subjectBinding the object binding of the subject that contains the relevant feature; must have a view object
     * @param context the context of the operation
     * @return the feature binding
     */
    FeatureBinding doGet(ObjectBinding subjectBinding, GetContext context);

    /**
     * Apply a change of the view towards the origin.
     *
     * @param change  the change of the view
     * @param feature the feature binding of the changed feature; must be a binding previously produced by this operation
     * @param subjectBinding the object binding of the subject that contains the relevant feature; must have a view object
     * @param value   the value update of the view feature
     * @param context the context of the operation
     * @return the new feature binding of the changed feature
     */
    FeatureBinding doPut(EChange<EObject> change, FeatureBinding feature, ObjectBinding subjectBinding, ValueUpdateBinding value, PutContext context);

    Optional<EChange<EObject>> doGetChange(EChange<EObject> change);
}
