package tools.vitruv.compmodelcons.views.impl;

import edu.kit.ipd.sdq.commons.util.org.eclipse.emf.ecore.resource.ResourceSetUtil;
import java.util.Collection;
import java.util.function.Function;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;

public class ViewResourceAccessImpl implements ViewResourceAccess {
  private final ResourceSet resourceSet;
  private final URI defaultViewUri;
  private final Function<String[], URI> metamodelUriSupplier;

  private Resource resource;

  public ViewResourceAccessImpl(URI defaultViewUri, Function<String[], URI> metamodelUriSupplier) {
    this.resourceSet = ResourceSetUtil.withGlobalFactories(new ResourceSetImpl());
    this.defaultViewUri = defaultViewUri;
    this.metamodelUriSupplier = metamodelUriSupplier;

    this.resource = resourceSet.createResource(defaultViewUri);
  }

  @Override
  public ResourceSet getResourceSet() {
    return resourceSet;
  }

  @Override
  public void reset() {
    resourceSet
        .getResources()
        .forEach(Resource::unload);
    resourceSet
        .getResources()
        .clear();

    resource = resourceSet.createResource(defaultViewUri);
  }

  @Override
  public void insertRoot(EObject root) {
    resource
        .getContents()
        .add(root);
  }

  @Override
  public void registerRoot(EObject root, URI uri) {
    if (resourceSet
        .getResources()
        .stream()
        .anyMatch(resource -> resource
            .getURI()
            .equals(uri))) {
      throw new IllegalStateException("That URI is already registered: " + uri);
    }

    resourceSet
        .createResource(uri)
        .getContents()
        .add(root);
  }

  @Override
  public void moveRoot(EObject root, URI uri) {
    resourceSet
        .getResources()
        .stream()
        .filter(resource -> resource
            .getContents()
            .contains(root))
        .findFirst()
        .ifPresent(resource -> resource.setURI(uri));
  }

  @Override
  public Collection<EObject> getRoots() {
    return resourceSet
        .getResources()
        .stream()
        .flatMap(resource -> resource
            .getContents()
            .stream())
        .toList();
  }

  @Override
  public void close() throws Exception {
    resourceSet
        .getResources()
        .forEach(Resource::unload);
    resourceSet
        .getResources()
        .clear();
  }

  @Override
  public URI getMetadataModelURI(String... strings) {
    return metamodelUriSupplier.apply(strings);
  }

  @Override
  public Resource getModelResource(URI uri) {
    Resource resource = resourceSet.getResource(uri, false);
    if (resource == null) {
      resource = resourceSet.createResource(uri);
    }
    return resource;
  }

  @Override
  public Collection<Resource> getModelResources() {
    return resourceSet.getResources();
  }

  @Override
  public void persistAsRoot(EObject eObject, URI uri) {
    if (!uri
        .fileExtension()
        .equals(defaultViewUri.fileExtension())) {
      throw new IllegalArgumentException(
          "View roots must be persisted using the view type metamodel's file extension ("
              + defaultViewUri.fileExtension() + "), but was " + uri.fileExtension());
    }
    registerRoot(eObject, uri);
  }
}
