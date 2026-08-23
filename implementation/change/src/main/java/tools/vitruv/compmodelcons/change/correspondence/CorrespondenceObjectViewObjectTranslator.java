package tools.vitruv.compmodelcons.change.correspondence;

import org.eclipse.emf.ecore.EObject;

/**
 * Translates between correspondence objects and view objects.
 * Correspondence objects are the objects that are stored in the correspondence model.
 * View objects cannot be stored in the correspondence model because views are temporary.
 */
public interface CorrespondenceObjectViewObjectTranslator extends AutoCloseable {
  boolean canResolveViewEObject(EObject viewObject);

  boolean canResolveCorrespondenceEObject(EObject correspondenceObject);

  EObject getViewEObject(EObject correspondenceEObject);

  EObject getCorrespondenceEObject(EObject viewEObject, boolean createIfNotExist);

  void onViewFitted();

  void onResolverUse();
}
