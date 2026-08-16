package tools.vitruv.compmodelcons.views.internal.impl;

import com.google.common.collect.Streams;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;

import java.util.Arrays;
import java.util.Collection;

public class ResourceAccessWrappingOriginResourceAccess extends AbstractOriginResourceAccess implements OriginResourceAccess {
    private final ResourceAccess resourceAccess;
    private final Resource[] additionalResources;

    public ResourceAccessWrappingOriginResourceAccess(ResourceAccess resourceAccess, Resource... additionalResources) {
        this.resourceAccess = resourceAccess;
        this.additionalResources = additionalResources;
        rebuildResourceMapping();
    }

    @Override
    protected Collection<EObject> getRoots() {
        return Streams.concat(resourceAccess.getModelResources().stream(), Arrays.stream(additionalResources))
                      .flatMap(resource -> resource.getContents().stream())
                      .distinct()
                      .toList();
    }

    @Override
    protected boolean canDeriveNameFromPackage(EPackage ePackage) {
        return Arrays.stream(additionalResources).noneMatch(resource -> resource.getContents().stream()
                                                                                .anyMatch(root -> root.eClass()
                                                                                                      .getEPackage() ==
                                                                                        ePackage));
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
