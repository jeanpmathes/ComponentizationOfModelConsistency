package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Functions;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureOriginBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueUpdateBinding;

/**
 * A feature origin operation that uses arbitrary, external functions to provide a feature.
 */
public class FeatureTransform implements FeatureOriginOperation {
  private final Functions.Function2<ObjectBinding, GetContext, FeatureOriginBinding> get;
  private final Functions.Function5<EChange<EObject>, FeatureOriginBinding, ObjectBinding,
      ValueUpdateBinding, PutContext, FeatureOriginBinding>
      put;

  public FeatureTransform(Functions.Function2<ObjectBinding, GetContext, FeatureOriginBinding> get,
                          Functions.Function5<EChange<EObject>, FeatureOriginBinding, ObjectBinding,
                              ValueUpdateBinding, PutContext, FeatureOriginBinding> put) {
    this.get = get;
    this.put = put;
  }

  @Override
  public FeatureOriginBinding doGet(ObjectBinding subjectBinding, GetContext context) {
    return get.apply(subjectBinding, context);
  }

  @Override
  public FeatureOriginBinding doPut(EChange<EObject> viewChange, FeatureOriginBinding feature,
                              ObjectBinding subjectBinding, ValueUpdateBinding value,
                              PutContext context) {
    return put.apply(viewChange, feature, subjectBinding, value, context);
  }
}
