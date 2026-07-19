package tools.vitruv.compmodelcons.views.internal.impl;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;

import java.util.*;

public abstract class AbstractOriginResourceAccess implements OriginResourceAccess {
    private final Map<EPackage, URI> knownDefaults = new java.util.HashMap<>();
    private final Map<EPackage, ResourceEntry> resources = new java.util.HashMap<>();

    @Override
    public Optional<Resource> getDefaultResource(EPackage ePackage) {
        ResourceEntry resourceEntry = resources.get(ePackage);
        if (resourceEntry != null) {
            return Optional.of(resourceEntry.defaultResource());
        }
        return Optional.empty();
    }

    @Override
    public Collection<Resource> getResources(EPackage ePackage) {
        ResourceEntry resourceEntry = resources.get(ePackage);
        if (resourceEntry != null) {
            return resourceEntry.allResources();
        }
        return List.of();
    }

    protected URI determineOriginUri(EPackage originPackage, URI uriHint) {
        return uriHint.trimFileExtension().appendFileExtension(originPackage.getNsPrefix());
    }

    @Override
    public Optional<URI> getViewUriHint(EPackage originPackage, EPackage viewtypePackage) {
        if (resources.isEmpty() || !resources.containsKey(originPackage)) {
            return Optional.empty();
        }

        return Optional.of(resources.get(originPackage).defaultResource().getURI().trimFileExtension().appendFileExtension(viewtypePackage.getNsPrefix()));
    }

    protected abstract Collection<EObject> getRoots();

    protected void rebuildResourceMapping() {
        knownDefaults.clear();
        resources.clear();

        for (EObject eObject : getRoots()) {
            EPackage ePackage = eObject.eClass().getEPackage();

            if (resources.containsKey(ePackage)) {
                resources.get(ePackage).allResources().add(eObject.eResource());
            } else {
                knownDefaults.put(ePackage, eObject.eResource().getURI());
                resources.put(ePackage, ResourceEntry.create(eObject));
            }
        }
    }

    @Override
    public void refreshResourceMapping() {
        resources.clear();

        Map<EPackage, Map<URI, Resource>> allResources = new HashMap<>();
        for (EObject eObject : getRoots()) {
            EPackage ePackage = eObject.eClass().getEPackage();
            allResources.computeIfAbsent(ePackage, ignore -> new HashMap<>()).put(eObject.eResource().getURI(), eObject.eResource());
        }

        for (var entry : allResources.entrySet()) {
            EPackage ePackage = entry.getKey();
            Map<URI, Resource> packageResources = entry.getValue();

            Optional.ofNullable(knownDefaults.get(ePackage))
                    .flatMap(uri -> Optional.ofNullable(packageResources.get(uri)))
                    .or(() -> packageResources.values().stream().findFirst())
                    .map(defaultResource -> new ResourceEntry(defaultResource, new HashSet<>(packageResources.values())))
                    .ifPresent(resourceEntry -> resources.put(ePackage, resourceEntry));
        }
    }

    private record ResourceEntry(Resource defaultResource, Set<Resource> allResources) {
        public static ResourceEntry create(EObject eObject) {
            return new ResourceEntry(eObject.eResource(), new HashSet<>(List.of(eObject.eResource())));
        }
    }
}
