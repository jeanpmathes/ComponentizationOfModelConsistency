package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;
import tools.vitruv.framework.views.View;

import java.util.*;

public class ViewWrappingOriginResourceAccessImpl implements OriginResourceAccess, AutoCloseable {
    private final View view;

    private final Map<EObject, ResourceEntry> resources = new java.util.HashMap<>();

    public ViewWrappingOriginResourceAccessImpl(View view) {
        this.view = view;
    }

    public View getView() {
        return view;
    }

    public void update() {
        resources.clear();

        view.update();

        for (EObject eObject : view.getRootObjects()) {
            resources.computeIfAbsent(eObject.eClass().getEPackage(), ResourceEntry::create).allResources().add(eObject.eResource());
        }
    }

    public boolean isOutdated() {
        return view.isOutdated();
    }

    @Override
    public Optional<Resource> getDefaultResource(EPackage ePackage) {
        ResourceEntry resourceEntry = resources.get(ePackage);
        if (resourceEntry != null) {
            return Optional.of(resourceEntry.defaultResource());
        }
        return Optional.empty();
    }

    @Override
    public void createResourceWithRoot(URI uriHint, EObject root) {
        view.registerRoot(root, determineOriginUri(root.eClass().getEPackage(), uriHint));
    }

    private URI determineOriginUri(EPackage originPackage, URI uriHint) {
        return uriHint.trimFileExtension().appendFileExtension(originPackage.getNsPrefix());
    }

    @Override
    public Collection<Resource> getResources(EPackage ePackage) {
        ResourceEntry resourceEntry = resources.get(ePackage);
        if (resourceEntry != null) {
            return resourceEntry.allResources();
        }
        return List.of();
    }

    @Override
    public void close() throws Exception {
        view.close();
    }

    private record ResourceEntry(Resource defaultResource, Set<Resource> allResources) {
        public static ResourceEntry create(EObject eObject) {
            Resource defaultResource = eObject.eResource();
            return new ResourceEntry(defaultResource, new HashSet<>());
        }
    }
}
