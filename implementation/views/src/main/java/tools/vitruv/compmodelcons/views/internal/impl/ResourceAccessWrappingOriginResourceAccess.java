package tools.vitruv.compmodelcons.views.internal.impl;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;

import java.util.Collection;

public class ResourceAccessWrappingOriginResourceAccess extends AbstractOriginResourceAccess implements OriginResourceAccess {
    private final ResourceAccess resourceAccess;

    public ResourceAccessWrappingOriginResourceAccess(ResourceAccess resourceAccess) {
        this.resourceAccess = resourceAccess;
        rebuildResourceMapping();
    }

    @Override
    protected Collection<EObject> getRoots() {
        return resourceAccess.getModelResources().stream()
                .flatMap(resource -> resource.getContents().stream())
                .distinct()
                .toList();
    }

    @Override
    public void createResourceWithRoot(URI uriHint, EObject root) {
        resourceAccess.persistAsRoot(root, determineOriginUri(root.eClass().getEPackage(), uriHint));
        refreshResourceMapping();
    }

    @Override
    public void close() {
        // Nothing to close, as we do not own the resource access.
    }
}
