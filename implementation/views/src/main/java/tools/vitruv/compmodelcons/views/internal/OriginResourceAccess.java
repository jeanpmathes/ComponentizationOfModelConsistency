package tools.vitruv.compmodelcons.views.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

public interface OriginResourceAccess extends AutoCloseable {
  Optional<Resource> getDefaultResource(EPackage ePackage);

  void createResourceWithRoot(URI uriHint, EObject root);

  Collection<Resource> getResources(EPackage ePackage);

  Optional<URI> getViewUriHint(List<EPackage> originPackages, EPackage viewtypePackage);

  void refreshResourceMapping();
}
