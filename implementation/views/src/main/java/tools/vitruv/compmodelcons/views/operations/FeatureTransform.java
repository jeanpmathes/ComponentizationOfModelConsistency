package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Functions;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueUpdateBinding;

public class FeatureTransform implements FeatureOriginOperation {
  private final Functions.Function2<ObjectBinding, GetContext, FeatureBinding> get;
  private final Functions.Function5<EChange<EObject>, FeatureBinding, ObjectBinding,
      ValueUpdateBinding, PutContext, FeatureBinding>
      put;

  public FeatureTransform(Functions.Function2<ObjectBinding, GetContext, FeatureBinding> get,
                          Functions.Function5<EChange<EObject>, FeatureBinding, ObjectBinding,
                              ValueUpdateBinding, PutContext, FeatureBinding> put) {
    this.get = get;
    this.put = put;
  }

  @Override
  public FeatureBinding doGet(ObjectBinding subjectBinding, GetContext context) {
    return get.apply(subjectBinding, context);
  }

  @Override
  public FeatureBinding doPut(EChange<EObject> viewChange, FeatureBinding feature,
                              ObjectBinding subjectBinding, ValueUpdateBinding value,
                              PutContext context) {
    return put.apply(viewChange, feature, subjectBinding, value, context);
  }
}
