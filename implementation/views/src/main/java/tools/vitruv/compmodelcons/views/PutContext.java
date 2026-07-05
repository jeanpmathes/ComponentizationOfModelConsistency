package tools.vitruv.compmodelcons.views;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

public interface PutContext extends Context {
    void addRootToOriginModel(EPackage originPackage, EObject originObject);

    void removeRootFromOriginModel(EPackage originPackage, EObject originObject);
}
