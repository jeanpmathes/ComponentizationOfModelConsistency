package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.compmodelcons.views.EditableViewCorrespondences;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;
import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;

import java.util.HashSet;
import java.util.Set;

public class PutContextImpl extends GetContextImpl implements PutContext {
    private final Set<EObject> unattachedCreatedOriginObjects = new HashSet<>();
    private final Set<EObject> undetachedDeletedOriginObjects = new HashSet<>();

    public PutContextImpl(OriginResourceAccess originResourceAccess, ViewResourceAccess viewResourceAccess, EditableViewCorrespondences correspondences) {
        super(originResourceAccess, viewResourceAccess, correspondences);
    }

    @Override
    public void addRootToDefaultOriginModel(EPackage originPackage, EObject originObject) {
        getOriginResourceAccess().getDefaultResource(originPackage).ifPresentOrElse(resource -> resource.getContents().add(originObject), () -> trackUnattachedCreatedOriginObject(originObject));
        trackOriginObjectAttachmentChange(originObject);
    }

    @Override
    public void removeRootFromDefaultOriginModel(EPackage originPackage, EObject originObject) {
        getOriginResourceAccess().getDefaultResource(originPackage).ifPresentOrElse(resource -> resource.getContents().remove(originObject), () -> trackUndetachedDeletedOriginObject(originObject));
        trackOriginObjectAttachmentChange(originObject);
    }

    @Override
    public void moveRootToOtherOriginModel(EPackage originPackage, EObject originObject, URI uriHint) {
        getOriginResourceAccess().createResourceWithRoot(uriHint, originObject);
        trackOriginObjectAttachmentChange(originObject);
    }

    @Override
    public void moveRootToDefaultOriginModel(EPackage originPackage, EObject originObject) {
        if (originObject.eResource() == null) {
            throw new IllegalArgumentException("Cannot move an origin object which is not already in a resource");
        }
        addRootToDefaultOriginModel(originPackage, originObject);
    }

    @Override
    public void trackUnattachedCreatedOriginObject(EObject originObject) {
        unattachedCreatedOriginObjects.add(originObject);
        undetachedDeletedOriginObjects.remove(originObject);
    }

    @Override
    public void trackUndetachedDeletedOriginObject(EObject originObject) {
        undetachedDeletedOriginObjects.add(originObject);
        unattachedCreatedOriginObjects.remove(originObject);
    }

    @Override
    public void trackOriginObjectAttachmentChange(EObject originObject) {
        if (originObject.eResource() != null) {
            unattachedCreatedOriginObjects.remove(originObject);
        } else {
            undetachedDeletedOriginObjects.remove(originObject);
        }
    }

    public void validateAttachmentState() {
        if (!unattachedCreatedOriginObjects.isEmpty()) {
            throw new IllegalStateException("Failed to attach all created objects in the origin models, possibly because of ambiguous containment");
        }
        if (!undetachedDeletedOriginObjects.isEmpty()) {
            throw new IllegalStateException("Failed to detach all deleted objects in the origin models, possibly because of ambiguous containment");
        }
    }
}
