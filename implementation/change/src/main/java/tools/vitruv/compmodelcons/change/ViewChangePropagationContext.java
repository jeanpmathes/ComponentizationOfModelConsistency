package tools.vitruv.compmodelcons.change;

import com.google.common.collect.Streams;
import java.util.Collection;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.utils.ResourceAccess;

public record ViewChangePropagationContext(ChangePropagationView sourceView,
                                           ChangePropagatingViewTypeSpecification sourceViewType,
                                           ChangePropagationView targetView,
                                           ChangePropagatingViewTypeSpecification targetViewType) {
  public ResourceAccess getResourceAccess() {
    return new CombinedViewResourceAccess();
  }

  private class CombinedViewResourceAccess implements ResourceAccess {
    @Override
    public URI getMetadataModelURI(String... strings) {
      return targetView
          .getViewResourceAccess()
          .getMetadataModelURI(strings);
    }

    @Override
    public Resource getModelResource(URI uri) {
      return getModelResources()
          .stream()
          .filter(resource -> resource
              .getURI()
              .equals(uri))
          .findFirst()
          .orElseGet(() -> targetView
              .getViewResourceAccess()
              .getModelResource(uri));
    }

    @Override
    public Collection<Resource> getModelResources() {
      return Streams
          .concat(
              sourceView
                  .getViewResourceAccess()
                  .getModelResources()
                  .stream(),
              targetView
                  .getViewResourceAccess()
                  .getModelResources()
                  .stream())
          .toList();
    }

    @Override
    public void persistAsRoot(EObject eObject, URI uri) {
      targetView
          .getViewResourceAccess()
          .persistAsRoot(eObject, uri);
    }
  }
}
