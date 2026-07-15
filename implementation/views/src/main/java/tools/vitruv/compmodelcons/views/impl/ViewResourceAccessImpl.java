package tools.vitruv.compmodelcons.views.impl;

import edu.kit.ipd.sdq.commons.util.org.eclipse.emf.ecore.resource.ResourceSetUtil;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;

import java.util.Collection;

public class ViewResourceAccessImpl implements ViewResourceAccess, AutoCloseable {
    private final ResourceSet resourceSet;
    private final String viewtypeName;

    private Resource resource;

    public ViewResourceAccessImpl(String viewtypeName) {
        this.resourceSet = ResourceSetUtil.withGlobalFactories(new ResourceSetImpl());
        this.viewtypeName = viewtypeName;
    }

    public ResourceSet getResourceSet() {
        return resourceSet;
    }

    public void reset() {
        resourceSet.getResources().forEach(Resource::unload);
        resourceSet.getResources().clear();

        resource = resourceSet.createResource(URI.createURI(String.format("view:/%s/default.view", viewtypeName)));
    }

    @Override
    public void insertRoot(EObject root) {
        resource.getContents().add(root);
    }

    @Override
    public void registerRoot(EObject root, URI uri) {
        resourceSet.createResource(uri).getContents().add(root);
    }

    @Override
    public void moveRoot(EObject root, URI uri) {
        resourceSet.getResources().stream()
                .filter(resource -> resource.getContents().contains(root))
                .findFirst()
                .ifPresent(resource -> resource.setURI(uri));
    }

    @Override
    public Collection<EObject> getRoots() {
        return resourceSet.getResources().stream()
                .flatMap(resource -> resource.getContents().stream())
                .toList();
    }

    @Override
    public void close() throws Exception {
        resourceSet.getResources().forEach(Resource::unload);
        resourceSet.getResources().clear();
    }
}
