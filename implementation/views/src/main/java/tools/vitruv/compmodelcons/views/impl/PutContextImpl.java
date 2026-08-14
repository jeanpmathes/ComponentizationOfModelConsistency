package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.compmodelcons.views.EditableViewCorrespondences;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.ViewObserver;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;
import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;

import java.util.HashSet;
import java.util.Set;

public class PutContextImpl extends GetContextImpl implements PutContext {
    private final Set<EObject> unattachedCreatedOriginObjects = new HashSet<>();
    private final Set<EObject> undetachedDeletedOriginObjects = new HashSet<>();

    private final ViewObserver viewObserver;

    public PutContextImpl(OriginResourceAccess originResourceAccess, ViewResourceAccess viewResourceAccess, EditableViewCorrespondences correspondences, ViewObserver viewObserver) {
        super(originResourceAccess, viewResourceAccess, correspondences);
        this.viewObserver = viewObserver;
    }

    @Override
    public void addRootToDefaultOriginModel(EPackage originPackage, EObject originObject) {
        getOriginResourceAccess().getDefaultResource(originPackage).ifPresentOrElse(resource -> resource.getContents()
                                                                                                        .add(originObject), () -> notifyUnattachedCreatedOriginObject(originObject));
        notifyOriginObjectAttachmentChange(originObject);
    }

    @Override
    public void removeRootFromDefaultOriginModel(EPackage originPackage, EObject originObject) {
        getOriginResourceAccess().getDefaultResource(originPackage).ifPresentOrElse(resource -> resource.getContents()
                                                                                                        .remove(originObject), () -> notifyUndetachedDeletedOriginObject(originObject));
        notifyOriginObjectAttachmentChange(originObject);
    }

    @Override
    public void moveRootToOtherOriginModel(EPackage originPackage, EObject originObject, URI uriHint) {
        getOriginResourceAccess().createResourceWithRoot(uriHint, originObject);
        notifyOriginObjectAttachmentChange(originObject);
    }

    @Override
    public void moveRootToDefaultOriginModel(EPackage originPackage, EObject originObject) {
        if (originObject.eResource() == null) {
            throw new IllegalArgumentException("Cannot move an origin object which is not already in a resource");
        }
        addRootToDefaultOriginModel(originPackage, originObject);
    }

    @Override
    public void notifyOriginObjectCreated(EObject originObject) {
        viewObserver.originObjectCreated(originObject);

        if (originObject.eResource() == null) {
            notifyUnattachedCreatedOriginObject(originObject);
        }
    }

    @Override
    public void notifyOriginObjectDeleted(EObject originObject) {
        viewObserver.originObjectDeleted(originObject);

        if (originObject.eResource() != null) {
            notifyUndetachedDeletedOriginObject(originObject);
        }
    }

    private void notifyUnattachedCreatedOriginObject(EObject originObject) {
        unattachedCreatedOriginObjects.add(originObject);
        undetachedDeletedOriginObjects.remove(originObject);
    }

    private void notifyUndetachedDeletedOriginObject(EObject originObject) {
        unattachedCreatedOriginObjects.remove(originObject);
        undetachedDeletedOriginObjects.add(originObject);
    }

    @Override
    public void notifyOriginObjectAttachmentChange(EObject originObject) {
        if (originObject.eResource() != null) {
            unattachedCreatedOriginObjects.remove(originObject);
        } else {
            undetachedDeletedOriginObjects.remove(originObject);
        }
    }

    public void validateAttachmentState() {
        // Any hooks and overrides might change attachment in code, so we check again just to be sure.
        unattachedCreatedOriginObjects.removeIf(originObject -> originObject.eResource() != null);
        undetachedDeletedOriginObjects.removeIf(originObject -> originObject.eResource() == null);

        if (!unattachedCreatedOriginObjects.isEmpty()) {
            throw new IllegalStateException("Failed to attach all created objects in the origin models, possibly because of ambiguous containment");
        }
        if (!undetachedDeletedOriginObjects.isEmpty()) {
            throw new IllegalStateException("Failed to detach all deleted objects in the origin models, possibly because of ambiguous containment");
        }
    }
}
