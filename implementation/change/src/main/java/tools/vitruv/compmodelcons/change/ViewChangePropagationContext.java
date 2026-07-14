package tools.vitruv.compmodelcons.change;

import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.utils.ResourceAccess;

import java.util.Collection;

public record ViewChangePropagationContext(ChangePropagationView sourceView,
                                           ChangePropagationViewTypeSpecification sourceViewType,
                                           ChangePropagationView targetView,
                                           ChangePropagationViewTypeSpecification targetViewType) implements ResourceAccess {
    @Override
    public URI getMetadataModelURI(String... strings) {
        throw new NotImplementedException(); // todo: decide whether view or target has to handle
    }

    @Override
    public Resource getModelResource(URI uri) {
        throw new NotImplementedException(); // todo: decide whether view or target has to handle
    }

    @Override
    public Collection<Resource> getModelResources() {
        throw new NotImplementedException(); // todo: decide whether view or target has to handle
    }

    @Override
    public void persistAsRoot(EObject eObject, URI uri) {
        throw new NotImplementedException(); // todo: decide whether view or target has to handle
    }
}
