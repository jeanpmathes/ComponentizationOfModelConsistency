package tools.vitruv.compmodelcons.views.internal;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.util.Collection;

public interface ViewResourceAccess extends AutoCloseable {
    void reset();

    ResourceSet getResourceSet();

    void insertRoot(EObject root);

    void registerRoot(EObject root, URI uri);

    void moveRoot(EObject root, URI uri);

    Collection<EObject> getRoots();
}
