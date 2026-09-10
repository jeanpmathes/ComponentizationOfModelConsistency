package tools.vitruv.compmodelcons.change;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.CreateEObject;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.change.atomic.feature.FeatureEChange;
import tools.vitruv.change.atomic.feature.UnsetFeature;
import tools.vitruv.change.atomic.feature.attribute.InsertEAttributeValue;
import tools.vitruv.change.atomic.feature.attribute.RemoveEAttributeValue;
import tools.vitruv.change.atomic.feature.attribute.ReplaceSingleValuedEAttribute;
import tools.vitruv.change.atomic.feature.list.InsertInListEChange;
import tools.vitruv.change.atomic.feature.list.RemoveFromListEChange;
import tools.vitruv.change.atomic.feature.reference.InsertEReference;
import tools.vitruv.change.atomic.feature.reference.RemoveEReference;
import tools.vitruv.change.atomic.feature.reference.ReplaceSingleValuedEReference;
import tools.vitruv.change.atomic.feature.single.ReplaceSingleValuedFeatureEChange;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.atomic.root.RemoveRootEObject;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.impl.AbstractChangePropagationSpecification;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.neojoin.utils.Pair;

public class ConformingMetamodelChangePropagationSpecification
    extends AbstractChangePropagationSpecification {
  private final EPackage sourceMetamodel;
  private final EPackage targetMetamodel;

  public ConformingMetamodelChangePropagationSpecification(
      EPackage sourceMetamodel, EPackage targetMetamodel) {
    super(MetamodelDescriptor.of(sourceMetamodel), MetamodelDescriptor.of(targetMetamodel));
    this.sourceMetamodel = sourceMetamodel;
    this.targetMetamodel = targetMetamodel;
  }

  @Override
  public boolean doesHandleChange(
      EChange<EObject> eChange,
      EditableCorrespondenceModelView<Correspondence> correspondences) {
    return isInSourceMetamodel(DynamicModels.getAffectedEObject(eChange));
  }

  @Override
  public void propagateChange(
      EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess) {
    switch (eChange) {
      case CreateEObject<EObject> ignored:
        // Is handled by inserting changes.
        break;
      case DeleteEObject<EObject> deleteEObject:
        deleteEObject(deleteEObject, correspondences);
        break;
      case InsertRootEObject<EObject> insertRootEObject:
        insertRootEObject(insertRootEObject, correspondences, resourceAccess);
        break;
      case RemoveRootEObject<EObject> removeRootEObject:
        removeRootEObject(removeRootEObject, correspondences, resourceAccess);
        break;
      case UnsetFeature<EObject, ?> unsetFeature:
        unsetFeature(unsetFeature, correspondences, resourceAccess);
        break;
      case InsertEAttributeValue<EObject, ?> insertEAttributeValue:
        insertEAttributeValue(insertEAttributeValue, correspondences, resourceAccess);
        break;
      case RemoveEAttributeValue<EObject, ?> removeEAttributeValue:
        removeEAttributeValue(removeEAttributeValue, correspondences, resourceAccess);
        break;
      case ReplaceSingleValuedEAttribute<EObject, ?> replaceSingleValuedEAttribute:
        replaceSingleValuedEAttribute(replaceSingleValuedEAttribute, correspondences,
                                      resourceAccess);
        break;
      case InsertEReference<EObject> insertEReference:
        insertEReference(insertEReference, correspondences, resourceAccess);
        break;
      case RemoveEReference<EObject> removeEReference:
        removeEReference(removeEReference, correspondences, resourceAccess);
        break;
      case ReplaceSingleValuedEReference<EObject> replaceSingleValuedEReference:
        replaceSingleValuedEReference(replaceSingleValuedEReference, correspondences,
                                      resourceAccess);
        break;
      default:
        throw new UnsupportedOperationException("Change type not supported: " + eChange.getClass());
    }
  }

  private boolean isInSourceMetamodel(EObject eObject) {
    return eObject.eClass().getEPackage().equals(sourceMetamodel);
  }

  private void deleteEObject(
      DeleteEObject<EObject> deleteEObject,
      EditableCorrespondenceModelView<Correspondence> correspondences) {
    EObject sourceEObject = deleteEObject.getAffectedElement();
    EObject targetEObject = getCorrespondingTarget(sourceEObject, correspondences);

    if (targetEObject == null) {
      return;
    }

    correspondences.removeCorrespondencesBetween(sourceEObject, targetEObject, "");
    EcoreUtil.remove(targetEObject);
  }

  private void insertRootEObject(
      InsertRootEObject<EObject> insertRootEObject,
      EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess) {
    EObject sourceEObject = insertRootEObject.getNewValue();
    EObject targetEObject = getCorrespondingTarget(sourceEObject, correspondences);

    if (targetEObject == null) {
      targetEObject = getCorrespondingTarget(sourceEObject, correspondences, true);
    } else {
      return;
    }

    URI sourceURI = insertRootEObject.getResource().getURI();
    URI targetURI = getTargetURI(sourceURI);

    resourceAccess.persistAsRoot(targetEObject, targetURI);
  }

  private void removeRootEObject(
      RemoveRootEObject<EObject> removeRootEObject,
      EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess) {
    EObject sourceEObject = removeRootEObject.getOldValue();
    EObject targetEObject = getCorrespondingTarget(sourceEObject, correspondences);

    if (targetEObject == null) {
      return;
    }

    Resource sourceResource = removeRootEObject.getResource();
    Resource targetResource = getTargetResource(sourceResource, resourceAccess);

    targetResource.getContents().remove(targetEObject);
  }

  private void unsetFeature(
      UnsetFeature<EObject, ?> unsetFeature,
      EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess) {
    updateFeature(unsetFeature, correspondences, resourceAccess, (features, eObjects) -> {
      EStructuralFeature targetFeature = features.right();
      EObject targetEObject = Objects.requireNonNull(eObjects.right());

      targetEObject.eUnset(targetFeature);
    });
  }

  private void insertEAttributeValue(
      InsertEAttributeValue<EObject, ?> insertEAttributeValue,
      EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess) {
    Object value = insertEAttributeValue.getNewValue();

    insertInList(insertEAttributeValue, value, correspondences, resourceAccess);
  }

  private void removeEAttributeValue(
      RemoveEAttributeValue<EObject, ?> removeEAttributeValue,
      EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess) {
    Object value = removeEAttributeValue.getOldValue();

    removeFromList(removeEAttributeValue, value, correspondences, resourceAccess);
  }

  private void replaceSingleValuedEAttribute(
      ReplaceSingleValuedEAttribute<EObject, ?> replaceSingleValuedEAttribute,
      EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess) {
    Object value = replaceSingleValuedEAttribute.getNewValue();

    replaceSingleValuedFeature(replaceSingleValuedEAttribute, value, correspondences,
                               resourceAccess);
  }

  private void insertEReference(
      InsertEReference<EObject> insertEReference,
      EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess) {
    EObject sourceValue = insertEReference.getNewValue();
    EObject targetValue = getCorrespondingTarget(sourceValue, correspondences,
                                                 insertEReference.getAffectedFeature()
                                                     .isContainment());

    if (targetValue == null) {
      return;
    }

    insertInList(insertEReference, targetValue, correspondences, resourceAccess);
  }

  private void removeEReference(
      RemoveEReference<EObject> removeEReference,
      EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess) {
    EObject sourceValue = removeEReference.getOldValue();
    EObject targetValue = getCorrespondingTarget(sourceValue, correspondences);

    if (targetValue == null) {
      return;
    }

    removeFromList(removeEReference, targetValue, correspondences, resourceAccess);
  }

  private void replaceSingleValuedEReference(
      ReplaceSingleValuedEReference<EObject> replaceSingleValuedEReference,
      EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess) {
    EObject sourceValue = replaceSingleValuedEReference.getOldValue();
    EObject targetValue = getCorrespondingTarget(sourceValue, correspondences,
                                                 replaceSingleValuedEReference.getAffectedFeature()
                                                     .isContainment());

    if (targetValue == null) {
      return;
    }

    replaceSingleValuedFeature(replaceSingleValuedEReference, targetValue, correspondences,
                               resourceAccess);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void insertInList(
      InsertInListEChange<EObject, ?, ?> insertInListEChange, Object value,
      EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess) {
    updateFeature(insertInListEChange, correspondences, resourceAccess, (features, eObjects) -> {
      EStructuralFeature sourceFeature = features.left();
      EStructuralFeature targetFeature = features.right();

      EObject sourceEObject = Objects.requireNonNull(eObjects.left());
      EObject targetEObject = Objects.requireNonNull(eObjects.right());

      List sourceList = (List) sourceEObject.eGet(sourceFeature);
      if (!(targetEObject.eGet(targetFeature) instanceof List targetList)) {
        propagateNonConformingChange(insertInListEChange, correspondences, resourceAccess);
        return;
      }

      int index = insertInListEChange.getIndex();

      if (index == -1) {
        targetList.add(value);
      } else {
        if (index < 0 || index > sourceList.size()) {
          propagateNonConformingChange(insertInListEChange, correspondences, resourceAccess);
          return;
        }
        insertAt(targetList, value, index);
      }
    });
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void removeFromList(
      RemoveFromListEChange<EObject, ?, ?> removeFromListEChange, Object value,
      EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess) {
    updateFeature(removeFromListEChange, correspondences, resourceAccess, (features, eObjects) -> {
      EStructuralFeature sourceFeature = features.left();
      EStructuralFeature targetFeature = features.right();

      EObject sourceEObject = Objects.requireNonNull(eObjects.left());
      EObject targetEObject = Objects.requireNonNull(eObjects.right());

      List sourceList = (List) sourceEObject.eGet(sourceFeature);
      if (!(targetEObject.eGet(targetFeature) instanceof List targetList)) {
        propagateNonConformingChange(removeFromListEChange, correspondences, resourceAccess);
        return;
      }

      int index = removeFromListEChange.getIndex();

      if (index == -1) {
        if (!targetList.remove(value)) {
          propagateNonConformingChange(removeFromListEChange, correspondences, resourceAccess);
        }
      } else {
        if (index < 0 || index > sourceList.size()) {
          propagateNonConformingChange(removeFromListEChange, correspondences, resourceAccess);
          return;
        }
        if (targetList.get(index) != value) {
          propagateNonConformingChange(removeFromListEChange, correspondences, resourceAccess);
          return;
        }
        targetList.remove(index);
      }
    });
  }

  private void replaceSingleValuedFeature(
      ReplaceSingleValuedFeatureEChange<EObject, ?, ?> replaceSingleValuedFeatureEChange,
      Object value, EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess) {
    updateFeature(replaceSingleValuedFeatureEChange, correspondences, resourceAccess,
                  (features, eObjects) -> {
                    EStructuralFeature targetFeature = features.right();
                    EObject targetEObject = Objects.requireNonNull(eObjects.right());

                    targetEObject.eSet(targetFeature, value);
                  });
  }

  private <T extends FeatureEChange<EObject, ?>> void updateFeature(
      T featureEChange, EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess,
      BiConsumer<Pair<EStructuralFeature, EStructuralFeature>, Pair<EObject, EObject>> continuation) {
    EStructuralFeature sourceFeature = featureEChange.getAffectedFeature();
    EStructuralFeature targetFeature = getTargetFeature(sourceFeature);

    if (targetFeature == null) {
      propagateNonConformingChange(featureEChange, correspondences, resourceAccess);
      return;
    }

    EObject sourceEObject = featureEChange.getAffectedElement();
    EObject targetEObject = getCorrespondingTarget(sourceEObject, correspondences);

    if (targetEObject == null) {
      return;
    }

    continuation.accept(new Pair<>(sourceFeature, targetFeature),
                        new Pair<>(sourceEObject, targetEObject));
  }

  private EStructuralFeature getTargetFeature(EStructuralFeature sourceFeature) {
    EClass targetClass = getTargetClass(sourceFeature.getEContainingClass());
    if (targetClass != null) {
      return targetClass.getEStructuralFeature(sourceFeature.getName());
    }
    return null;
  }

  protected void propagateNonConformingChange(
      EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> correspondences,
      ResourceAccess resourceAccess) {
    throw new UnsupportedOperationException("Cannot propagate change: " + eChange);
  }

  protected EObject getCorrespondingTarget(
      EObject sourceEObject,
      EditableCorrespondenceModelView<Correspondence> correspondences) {
    Set<List<EObject>> correspondingEObjects =
        correspondences.getCorrespondingEObjects(List.of(sourceEObject), null);
    List<EObject> candidates = new ArrayList<>();
    for (List<EObject> eObjects : correspondingEObjects) {
      if (eObjects.size() == 1 && isInTargetMetamodel(eObjects.getFirst())) {
        candidates.add(eObjects.getFirst());
      }
    }
    if (candidates.isEmpty()) {
      return null;
    }
    if (candidates.size() == 1) {
      return candidates.getFirst();
    }
    throw new IllegalStateException("Multiple candidates for " + sourceEObject + ": " + candidates);
  }

  protected EObject getCorrespondingTarget(
      EObject sourceEObject, EditableCorrespondenceModelView<Correspondence> correspondences,
      boolean allowCreation) {
    EObject targetEObject = getCorrespondingTarget(sourceEObject, correspondences);
    if (targetEObject == null && allowCreation) {
      EClass sourceClass = sourceEObject.eClass();
      EClass targetClass = getTargetClass(sourceClass);

      targetEObject = targetClass.getEPackage().getEFactoryInstance().create(targetClass);
      String tag = getCorrespondenceTag(sourceEObject, targetEObject);
      correspondences.addCorrespondenceBetween(sourceEObject, targetEObject, tag);
    }
    return targetEObject;
  }

  protected String getCorrespondenceTag(EObject leftEObject, EObject rightEObject) {
    return "";
  }

  private EClass getTargetClass(EClass sourceClass) {
    if (targetMetamodel.getEClassifier(sourceClass.getName()) instanceof EClass targetClass) {
      return targetClass;
    }
    return null;
  }

  private <T> void insertAt(List<T> list, T entry, int index) {
    if (index >= list.size() || !list.get(index).equals(entry)) {
      list.add(index, entry);
    }
  }

  private boolean isInTargetMetamodel(EObject eObject) {
    return eObject.eClass().getEPackage().equals(targetMetamodel);
  }

  private Resource getTargetResource(Resource sourceResource, ResourceAccess resourceAccess) {
    URI sourceURI = sourceResource.getURI();
    URI targetURI = getTargetURI(sourceURI);

    return resourceAccess.getModelResource(targetURI);
  }

  private URI getTargetURI(URI sourceURI) {
    return sourceURI.trimFileExtension().appendFileExtension(targetMetamodel.getNsPrefix());
  }
}
