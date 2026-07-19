package tools.vitruv.compmodelcons.change.impl;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.compmodelcons.change.CorrespondenceResolver;

public class PassthroughCorrespondenceResolverImpl implements CorrespondenceResolver {
    private final MetamodelDescriptor metamodel;

    public PassthroughCorrespondenceResolverImpl(MetamodelDescriptor metamodel) {
        this.metamodel = metamodel;
    }

    private boolean canResolveEPackage(EPackage ePackage) {
        return metamodel.getNsUris().contains(ePackage.getNsURI());
    }

    @Override
    public boolean canResolveViewEObject(EObject viewObject) {
        return canResolveEPackage(viewObject.eClass().getEPackage());
    }

    @Override
    public boolean canResolveCorrespondenceEObject(EObject correspondenceObject) {
        return canResolveEPackage(correspondenceObject.eClass().getEPackage());
    }

    @Override
    public EObject getViewEObject(EObject correspondenceEObject) {
        return correspondenceEObject;
    }

    @Override
    public EObject getCorrespondenceEObject(EObject viewEObject, boolean createIfNotExist) {
        return viewEObject;
    }

    @Override
    public void close() {

    }
}
