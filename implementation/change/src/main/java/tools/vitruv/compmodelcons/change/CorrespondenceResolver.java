package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.ecore.EObject;

public interface CorrespondenceResolver extends AutoCloseable {
    boolean canResolveViewEObject(EObject viewObject);

    boolean canResolveCorrespondenceEObject(EObject correspondenceObject);

    EObject getViewEObject(EObject correspondenceEObject);

    EObject getCorrespondenceEObject(EObject viewEObject, boolean createIfNotExist);
}
