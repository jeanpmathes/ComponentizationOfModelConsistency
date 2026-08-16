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
  private final Functions.Function4<FeatureBinding, ObjectBinding, EChange<EObject>, GetContext,
      FeatureBinding>
      updatingGet;

  public FeatureTransform(Functions.Function2<ObjectBinding, GetContext, FeatureBinding> get,
                          Functions.Function5<EChange<EObject>, FeatureBinding, ObjectBinding,
                              ValueUpdateBinding, PutContext, FeatureBinding> put,
                          Functions.Function4<FeatureBinding, ObjectBinding, EChange<EObject>,
                              GetContext, FeatureBinding> updatingGet) {
    this.get = get;
    this.put = put;
    this.updatingGet = updatingGet;
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

  @Override
  public FeatureBinding doUpdatingGet(FeatureBinding previous, ObjectBinding subjectBinding,
                                      EChange<EObject> originChange, GetContext context) {
    return updatingGet.apply(previous, subjectBinding, originChange, context);
  }
}
