package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.compmodelcons.views.ViewObserver;

public class DefaultViewObserver implements ViewObserver {
    public static final DefaultViewObserver INSTANCE = new DefaultViewObserver();

    @Override
    public void originObjectCreated(EObject eObject) {

    }
}
