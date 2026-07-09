package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.Optional;

public interface FeatureOperation {
    FeatureBinding GET(ObjectBinding subject, GetContext context);

    FeatureBinding PUT(EChange<EObject> change, FeatureBinding feature, ObjectBinding subject, Optional<Object> value, PutContext context);

    Optional<EChange<EObject>> GET_CHANGE(EChange<EObject> change);
}
