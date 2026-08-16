package tools.vitruv.compmodelcons.views.operations;

import java.util.List;
import java.util.Optional;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.FeatureOriginBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueUpdateBinding;

/**
 * An origin operation targeting a feature of an origin object.
 */
public class FeatureSource implements FeatureOriginOperation {
  private final Target target;
  private final boolean isSourceFeatureAContainmentFeature;

  /**
   * Creates a new feature source operation.
   *
   * @param target the target of the operation
   */
  public FeatureSource(Target target) {
    assert !target.features.isEmpty();

    this.target = target;

    if (target.features.getLast() instanceof EReference eReference) {
      this.isSourceFeatureAContainmentFeature = eReference.isContainment();
    } else {
      this.isSourceFeatureAContainmentFeature = false;
    }
  }

  @Override
  public FeatureBinding doGet(ObjectBinding subjectBinding, GetContext context) {
    return target.get(subjectBinding);
  }

  public FeatureBinding doPut(EChange<EObject> viewChange, FeatureOriginBinding feature,
                              ObjectBinding subjectBinding, ValueUpdateBinding value,
                              PutContext context) {
    EObject subject = subjectBinding
        .originObjects()
        .get(target.index());
    Optional<Target.Access> access = target.access(subjectBinding);

    if (access.isEmpty()) {
      throw new IllegalArgumentException("Cannot put a change on a feature that is not accessible");
    }

    Object object = null;

    switch (value) {
      case ValueUpdateBinding.Unset ignored -> access
          .get()
          .eObject()
          .eUnset(access
                      .get()
                      .eStructuralFeature());
      case ValueUpdateBinding.Replace(Object newValue) -> {
        access
            .get()
            .eObject()
            .eSet(access
                      .get()
                      .eStructuralFeature(), newValue);
        object = newValue;
      }
      case ValueUpdateBinding.Insert(Object inserted, int index) -> {
        //noinspection unchecked
        var list = ((List<Object>) access
            .get()
            .eObject()
            .eGet(access
                      .get()
                      .eStructuralFeature()));
        ValueUpdateBinding.insert(list, inserted, index);
        object = inserted;
      }
      case ValueUpdateBinding.Remove(Object removed, int index) -> {
        //noinspection unchecked
        var list = ((List<Object>) access
            .get()
            .eObject()
            .eGet(access
                      .get()
                      .eStructuralFeature()));
        ValueUpdateBinding.remove(list, removed, index);
        object = removed;
      }
      default -> throw new IllegalArgumentException(
          "Unsupported value update type: " + value
              .getClass()
              .getSimpleName());
    }

    if (isSourceFeatureAContainmentFeature && object instanceof EObject eObject) {
      context.notifyOriginObjectAttachmentChange(eObject);
    }

    return FeatureBinding.ofOriginObject(subject, ValueBinding.ofFeature(access
                                                                             .get()
                                                                             .eObject(), access
                                                                             .get()
                                                                             .eStructuralFeature()));
  }

  /**
   * The target of the operation.
   *
   * @param index    the index of the origin object in the join sequence
   * @param features the chain of features to access, the last feature is the targeted feature
   */
  public record Target(int index, List<EStructuralFeature> features) {
    public static Target ofFirst(EStructuralFeature feature) {
      return new Target(0, List.of(feature));
    }

    private Optional<Access> access(ObjectBinding subjectBinding) {
      EObject current = subjectBinding
          .originObjects()
          .get(index);
      EStructuralFeature currentFeature = features.getFirst();

      for (int featureIndex = 0; featureIndex < features.size() - 1; featureIndex++) {
        if (current.eIsSet(currentFeature)) {
          Object next = current.eGet(currentFeature);
          if (next instanceof EObject eObject) {
            current = eObject;
            currentFeature = features.get(featureIndex + 1);
          } else {
            break;
          }
        } else {
          return Optional.empty();
        }
      }

      return Optional.of(new Access(current, currentFeature));
    }

    public FeatureBinding get(ObjectBinding subjectBinding) {
      EObject subject = subjectBinding
          .originObjects()
          .get(index);

      return FeatureBinding.ofOriginObject(subject, access(subjectBinding)
          .map(access -> ValueBinding.ofFeature(access.eObject(), access.eStructuralFeature()))
          .orElseGet(ValueBinding.Unset::new));
    }

    private record Access(EObject eObject, EStructuralFeature eStructuralFeature) {
    }
  }
}
