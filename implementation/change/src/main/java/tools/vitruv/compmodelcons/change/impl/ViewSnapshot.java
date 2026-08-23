package tools.vitruv.compmodelcons.change.impl;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.ModelRepositorySnapshot;
import tools.vitruv.change.propagation.impl.DefaultModelRepositorySnapshot;
import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;

public class ViewSnapshot implements ModelRepositorySnapshot {
  private final ResourceSet resourceSet;
  private final Function<String[], URI> metadataModelUriProvider;
  private final EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView;

  public ViewSnapshot(ViewResourceAccess viewResourceAccess,
                      EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView) {
    var copy = DefaultModelRepositorySnapshot.copyResourceSet(viewResourceAccess.getResourceSet());

    this.resourceSet = copy.resourceSet();
    this.metadataModelUriProvider = viewResourceAccess::getMetadataModelURI;
    this.editableCorrespondenceModelView = editableCorrespondenceModelView;
  }

  @Override
  public EditableCorrespondenceModelView<Correspondence> getCorrespondenceModel() {
    return editableCorrespondenceModelView;
  }

  @Override
  public URI getMetadataModelURI(String... strings) {
    return metadataModelUriProvider.apply(strings);
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
    return List.copyOf(resourceSet.getResources());
  }

  @Override
  public void persistAsRoot(EObject rootObject, URI uri) {
    Resource resource = getModelResource(uri);
    resource
        .getContents()
        .add(rootObject);
    resource.setModified(true);
  }

  @Override
  public void close() {
    for (Resource resource : resourceSet.getResources()) {
      resource.unload();
    }

    resourceSet
        .getResources()
        .clear();
  }
}
