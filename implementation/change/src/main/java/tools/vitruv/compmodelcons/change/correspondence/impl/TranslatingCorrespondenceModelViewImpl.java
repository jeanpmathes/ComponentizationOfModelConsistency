package tools.vitruv.compmodelcons.change.correspondence.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.CorrespondenceModelView;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceObjectViewObjectTranslator;

public class TranslatingCorrespondenceModelViewImpl<C extends Correspondence>
    implements CorrespondenceModelView<C> {
  protected final CorrespondenceModelView<C> inner;
  protected final CorrespondenceObjectViewObjectTranslator sourceTranslator;
  protected final CorrespondenceObjectViewObjectTranslator targetTranslator;

  public TranslatingCorrespondenceModelViewImpl(
      CorrespondenceModelView<C> inner,
      CorrespondenceObjectViewObjectTranslator sourceTranslator,
      CorrespondenceObjectViewObjectTranslator targetTranslator) {
    this.inner = inner;
    this.sourceTranslator = sourceTranslator;
    this.targetTranslator = targetTranslator;
  }

  @Override
  public Set<EObject> getAllEObjectsInACorrespondence() {
    Set<EObject> result = new HashSet<>();

    for (EObject correspondeceEObject : inner.getAllEObjectsInACorrespondence()) {
      EObject viewEObject = getViewEObject(correspondeceEObject);
      if (viewEObject != null) {
        result.add(viewEObject);
      }
    }

    return result;
  }

  @Override
  public Set<String> getAllTags() {
    return inner.getAllTags();
  }

  @Override
  public boolean hasCorrespondences(List<EObject> eObjects) {
    return !getCorrespondingEObjects(eObjects).isEmpty();
  }

  @Override
  public Set<List<EObject>> getCorrespondingEObjects(List<EObject> eObjects) {
    List<EObject> correspondenceObjects = getCorrespondenceEObjects(eObjects, false);
    if (correspondenceObjects == null) {
      return Set.of();
    }
    return getViewEObjects(inner.getCorrespondingEObjects(correspondenceObjects));
  }

  @Override
  public Set<List<EObject>> getCorrespondingEObjects(List<EObject> objects, String tag) {
    List<EObject> correspondenceObjects = getCorrespondenceEObjects(objects, false);
    if (correspondenceObjects == null) {
      return Set.of();
    }
    return getViewEObjects(inner.getCorrespondingEObjects(correspondenceObjects, tag));
  }

  @Override
  public Map<String, Set<EObject>> getCorrespondingEObjectsWithTag(List<EObject> list) {
    List<EObject> correspondenceObjects = getCorrespondenceEObjects(list, false);
    if (correspondenceObjects == null) {
      return Map.of();
    }

    Map<String, Set<EObject>> result = new HashMap<>();

    for (var entry : inner.getCorrespondingEObjectsWithTag(correspondenceObjects).entrySet()) {
      Set<EObject> viewEObjects = new HashSet<>();

      for (EObject correspondenceEObject : entry.getValue()) {
        EObject viewEObject = getViewEObject(correspondenceEObject);
        if (viewEObject != null) {
          viewEObjects.add(viewEObject);
        }
      }

      if (!viewEObjects.isEmpty()) {
        result.put(entry.getKey(), viewEObjects);
      }
    }

    return result;
  }

  @Override
  public <V extends C> CorrespondenceModelView<V> getView(Class<V> correspondenceType) {
    return new TranslatingCorrespondenceModelViewImpl<>(inner.getView(correspondenceType),
                                                        sourceTranslator, targetTranslator);
  }

  protected List<EObject> getCorrespondenceEObjects(
      List<EObject> eObjects,
      boolean createIfNotExist) {
    List<EObject> correspondenceEObjects = new ArrayList<>(eObjects.size());
    for (EObject eObject : eObjects) {
      EObject correspondenceObject = getCorrespondenceEObject(eObject, createIfNotExist);
      if (correspondenceObject == null) {
        return null;
      }
      correspondenceEObjects.add(correspondenceObject);
    }
    return correspondenceEObjects;
  }

  private EObject getCorrespondenceEObject(EObject viewEObject, boolean createIfNotExist) {
    if (sourceTranslator.canTranslateViewEObject(viewEObject)) {
      return sourceTranslator.translateCorrespondenceEObject(viewEObject, createIfNotExist);
    }
    if (targetTranslator.canTranslateViewEObject(viewEObject)) {
      return targetTranslator.translateCorrespondenceEObject(viewEObject, createIfNotExist);
    }
    return null;
  }

  private EObject getViewEObject(EObject correspondenceEObject) {
    if (sourceTranslator.canTranslateCorrespondenceEObject(correspondenceEObject)) {
      return sourceTranslator.translateViewEObject(correspondenceEObject);
    }
    if (targetTranslator.canTranslateCorrespondenceEObject(correspondenceEObject)) {
      return targetTranslator.translateViewEObject(correspondenceEObject);
    }
    return null;
  }

  protected Set<List<EObject>> getViewEObjects(Set<List<EObject>> eObjects) {
    Set<List<EObject>> result = new HashSet<>(eObjects.size());
    for (List<EObject> correspondenceEObjects : eObjects) {
      List<EObject> viewEObjects = new ArrayList<>(correspondenceEObjects.size());

      for (EObject correspondenceEObject : correspondenceEObjects) {
        EObject viewEObject = getViewEObject(correspondenceEObject);
        if (viewEObject == null) {
          viewEObjects = null;
          break;
        }
        viewEObjects.add(viewEObject);
      }

      if (viewEObjects != null) {
        result.add(viewEObjects);
      }
    }

    return result;
  }
}