package tools.vitruv.compmodelcons.views;

import org.eclipse.emf.ecore.EObject;

public interface ViewObserver {
    void originObjectCreated(EObject eObject);
}
