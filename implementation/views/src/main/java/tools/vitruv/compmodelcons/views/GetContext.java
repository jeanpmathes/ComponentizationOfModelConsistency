package tools.vitruv.compmodelcons.views;

import org.eclipse.emf.ecore.EObject;

public interface GetContext extends Context {
    void insertViewRoot(EObject root);
}
