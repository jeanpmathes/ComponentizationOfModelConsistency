package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueUpdateBinding;

public interface FeatureOriginOperation {
    FeatureBinding doGet(ObjectBinding subjectBinding, GetContext context);

    FeatureBinding doPut(EChange<EObject> viewChange, FeatureBinding feature, ObjectBinding subjectBinding, ValueUpdateBinding value, PutContext context);

    FeatureBinding doUpdatingGet(FeatureBinding feature, EChange<EObject> originChange, GetContext context);
}
