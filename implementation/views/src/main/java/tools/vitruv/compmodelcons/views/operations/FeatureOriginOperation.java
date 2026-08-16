package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureOriginBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueUpdateBinding;

/**
 * An origin operation providing a feature.
 * Origin operations are all operations that occur before view-side elements exist.
 */
public interface FeatureOriginOperation {
  FeatureOriginBinding doGet(ObjectBinding subjectBinding, GetContext context);

  FeatureOriginBinding doPut(EChange<EObject> viewChange, FeatureOriginBinding feature,
                             ObjectBinding subjectBinding, ValueUpdateBinding value,
                             PutContext context);
}
