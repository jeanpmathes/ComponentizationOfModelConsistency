package tools.vitruv.compmodelcons.change.correspondence.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.CorrespondenceModelView;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceResolver;

public class TranslatingCorrespondenceModelViewImpl<C extends Correspondence>
    implements CorrespondenceModelView<C> {
  protected final CorrespondenceModelView<C> inner;
  protected final CorrespondenceResolver sourceResolver;
  protected final CorrespondenceResolver targetResolver;

  public TranslatingCorrespondenceModelViewImpl(CorrespondenceModelView<C> inner,
                                                CorrespondenceResolver sourceResolver,
                                                CorrespondenceResolver targetResolver) {
    this.inner = inner;
    this.sourceResolver = sourceResolver;
    this.targetResolver = targetResolver;
  }

  @Override
  public boolean hasCorrespondences(List<EObject> eObjects) {
    List<EObject> correspondenceObjects = getCorrespondenceEObjects(eObjects, false);
    return correspondenceObjects != null && inner.hasCorrespondences(correspondenceObjects);
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
  public <V extends C> CorrespondenceModelView<V> getView(Class<V> correspondenceType) {
    return new TranslatingCorrespondenceModelViewImpl<>(inner.getView(correspondenceType),
                                                        sourceResolver, targetResolver);
  }

  protected List<EObject> getCorrespondenceEObjects(List<EObject> eObjects,
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
    if (sourceResolver.canResolveViewEObject(viewEObject)) {
      return sourceResolver.getCorrespondenceEObject(viewEObject, createIfNotExist);
    }
    if (targetResolver.canResolveViewEObject(viewEObject)) {
      return targetResolver.getCorrespondenceEObject(viewEObject, createIfNotExist);
    }
    return null;
  }

  protected Set<List<EObject>> getViewEObjects(Set<List<EObject>> eObjects) {
    Set<List<EObject>> viewEObjects = new HashSet<>(eObjects.size());
    for (List<EObject> eObject : eObjects) {
      viewEObjects.add(eObject
                           .stream()
                           .map(this::getViewEObject)
                           .toList());
    }
    return viewEObjects;
  }

  private EObject getViewEObject(EObject correspondenceEObject) {
    if (sourceResolver.canResolveCorrespondenceEObject(correspondenceEObject)) {
      return sourceResolver.getViewEObject(correspondenceEObject);
    }
    if (targetResolver.canResolveCorrespondenceEObject(correspondenceEObject)) {
      return targetResolver.getViewEObject(correspondenceEObject);
    }
    return null;
  }
}