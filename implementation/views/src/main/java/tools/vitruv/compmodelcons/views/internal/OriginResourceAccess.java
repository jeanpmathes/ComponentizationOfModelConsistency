package tools.vitruv.compmodelcons.views.internal;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

import java.util.Collection;
import java.util.Optional;

public interface OriginResourceAccess extends AutoCloseable {
    Optional<Resource> getDefaultResource(EPackage ePackage);

    void createResourceWithRoot(URI uriHint, EObject root);

    Collection<Resource> getResources(EPackage ePackage);

    Optional<URI> getViewUriHint(EPackage originPackage, EPackage viewtypePackage);

    void refreshResourceMapping();
}
