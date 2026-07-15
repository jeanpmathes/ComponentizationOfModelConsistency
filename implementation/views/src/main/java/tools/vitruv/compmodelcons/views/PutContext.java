package tools.vitruv.compmodelcons.views;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

public interface PutContext extends GetContext {
    void addRootToDefaultOriginModel(EPackage originPackage, EObject originObject);

    void removeRootFromDefaultOriginModel(EPackage originPackage, EObject originObject);

    void moveRootToOtherOriginModel(EPackage originPackage, EObject originObject, URI uriHint);

    void moveRootToDefaultOriginModel(EPackage originPackage, EObject originObject);

    void trackUnattachedCreatedOriginObject(EObject originObject);

    void trackUndetachedDeletedOriginObject(EObject originObject);

    void trackOriginObjectAttachmentChange(EObject originObject);
}
