package tools.vitruv.compmodelcons.views.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.FeatureEChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.OriginBinding;

public class Project {
  private final EClass createdClass;
  private final OriginOperation origin;
  private final List<FeatureProject> features;
  private final OnPut onPut;

  private final Map<EStructuralFeature, Integer> featureIndices = new java.util.HashMap<>();

  public Project(EClass createdClass, OriginOperation origin, List<FeatureProject> features,
                 OnPut onPut) {
    this.createdClass = createdClass;
    this.origin = origin;
    this.features = List.copyOf(features);
    this.onPut = onPut;

    for (int index = 0; index < features.size(); index++) {
      featureIndices.put(features
                             .get(index)
                             .getCreatedFeature(), index);
    }
  }

  public List<ObjectBinding> beginGetByCreatingViewObjects(GetContext context) {
    return origin
        .doGet(context)
        .stream()
        .map(originBinding -> {
          EObject result = createdClass
              .getEPackage()
              .getEFactoryInstance()
              .create(createdClass);

          context
              .getCorrespondences()
              .addCorrespondence(originBinding.originObjects(), result);

          return (ObjectBinding) new ProjectObjectBindingImpl(originBinding, result,
                                                              createUninitializedFeatureBindings());
        })
        .toList();
  }

  public void completeGetByCallingGetOnFeatures(ObjectBinding subject, GetContext context) {
    ProjectObjectBindingImpl projected = (ProjectObjectBindingImpl) subject;

    for (int index = 0; index < projected.featureBindings.size(); index++) {
      projected.featureBindings.set(index, features
          .get(index)
          .doGet(subject, context));
    }
  }

  public ObjectBinding doPut(EChange<EObject> viewChange, ObjectBinding subject,
                             PutContext context) {
    if (!subject
        .viewObject()
        .eClass()
        .equals(createdClass)) {
      throw new IllegalArgumentException(
          "Cannot put a change on an object that is not of the created class");
    }

    EObject viewObject = subject.viewObject();
    OriginBinding peeledTarget = OriginBinding.of(subject.originObjects());
    List<FeatureBinding> featureBindings;

    if (!subject
        .originObjects()
        .isEmpty()) {
      ProjectObjectBindingImpl binding = (ProjectObjectBindingImpl) subject;
      peeledTarget = binding.originBinding;
      featureBindings = new ArrayList<>(binding.featureBindings);
    } else {
      featureBindings = createUninitializedFeatureBindings();
    }

    if (viewChange instanceof FeatureEChange<EObject, ?> featureEChange) {
      int featureIndex = featureIndices.get(featureEChange.getAffectedFeature());
      featureBindings.set(featureIndex, features
          .get(featureIndex)
          .doPut(viewChange, featureBindings.get(featureIndex), subject, context));
      ProjectObjectBindingImpl projected =
          new ProjectObjectBindingImpl(peeledTarget, viewObject, featureBindings);
      onPut.onPut(viewChange, subject, projected, context);
      return projected;
    } else {
      OriginBinding originBinding = origin.doPut(viewChange, peeledTarget, context);

      ProjectObjectBindingImpl projected =
          new ProjectObjectBindingImpl(originBinding, viewObject, featureBindings);

      if (subject
          .originObjects()
          .isEmpty()) {
        for (int index = 0; index < featureBindings.size(); index++) {
          featureBindings.set(index, features
              .get(index)
              .initializeBindingFromView(projected, context));
        }
      }

      onPut.onPut(viewChange, subject, projected, context);

      return projected;
    }
  }

  private List<FeatureBinding> createUninitializedFeatureBindings() {
    List<FeatureBinding> result = new ArrayList<>(features.size());

    for (int index = 0; index < features.size(); index++) {
      result.add(null);
    }

    return result;
  }

  public interface OnPut {
    OnPut NO_OP = (change, oldBinding, newBinding, context) -> {
    };
    void onPut(EChange<EObject> change, OriginBinding oldBinding, OriginBinding newBinding,
               PutContext context);
  }

  private record ProjectObjectBindingImpl(OriginBinding originBinding, EObject viewObject,
                                          List<FeatureBinding> featureBindings)
      implements ObjectBinding {

    @Override
    public List<EObject> originObjects() {
      return originBinding.originObjects();
    }
  }
}
