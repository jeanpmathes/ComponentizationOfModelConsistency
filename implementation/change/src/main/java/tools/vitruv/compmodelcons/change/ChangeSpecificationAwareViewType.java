package tools.vitruv.compmodelcons.change;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.changederivation.StateBasedChangeResolutionStrategy;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.description.VitruviusChangeFactory;
import tools.vitruv.change.composite.description.VitruviusChangeResolverFactory;
import tools.vitruv.change.propagation.ChangePropagationObservable;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceResolver;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceResolverFactory;
import tools.vitruv.compmodelcons.change.impl.RootPreservingStateBasedChangeResolutionStrategy;
import tools.vitruv.compmodelcons.views.impl.DefaultViewObserver;
import tools.vitruv.compmodelcons.views.impl.OperationBasedViewType;
import tools.vitruv.compmodelcons.views.impl.ViewResourceAccessImpl;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;
import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;
import tools.vitruv.compmodelcons.views.internal.impl.InternalViewImpl;
import tools.vitruv.compmodelcons.views.internal.impl.ResourceAccessWrappingOriginResourceAccess;

/**
 * Abstract base class for view types that can participate in change propagation.
 */
public abstract class ChangeSpecificationAwareViewType extends OperationBasedViewType
    implements ChangePropagatingViewTypeSpecification {
  public ChangeSpecificationAwareViewType(String name, List<EPackage> originMetamodels,
                                          EPackage viewTypeMetamodel) {
    super(name, originMetamodels, viewTypeMetamodel);
  }

  @Override
  public MetamodelDescriptor getOriginMetamodelDescriptor() {
    return MetamodelDescriptor.of(new HashSet<>(getOriginMetamodels()));
  }

  @Override
  public MetamodelDescriptor getViewTypeMetamodelDescriptor() {
    return MetamodelDescriptor.of(getMetamodel());
  }

  @Override
  public ChangePropagationView createView(ResourceAccess resourceAccess,
                                          CorrespondenceModelAccess correspondenceModelAccess,
                                          Function<String, URI> uriFactory,
                                          ChangePropagationObservable observable,
                                          CorrespondenceResolverFactory correspondenceResolverFactory) {
    return new ChangePropagationViewImpl(resourceAccess, correspondenceModelAccess,
                                         createUri(uriFactory), observable,
                                         correspondenceResolverFactory);
  }

  private class ChangePropagationViewImpl implements ChangePropagationView {
    private final OriginResourceAccess originResourceAccess;
    private final ViewResourceAccess viewResourceAccess;
    private final InternalViewImpl internalView;
    private final URI viewUri;
    private final CorrespondenceResolver correspondenceResolver;

    public ChangePropagationViewImpl(ResourceAccess resourceAccess,
                                     CorrespondenceModelAccess correspondenceModelAccess,
                                     URI viewUri, ChangePropagationObservable observable,
                                     CorrespondenceResolverFactory correspondenceResolverFactory) {
      this.originResourceAccess = new ResourceAccessWrappingOriginResourceAccess(resourceAccess,
                                                                                 correspondenceModelAccess.getResource());
      this.viewUri = this.originResourceAccess
          .getViewUriHint(getOriginMetamodels(), getMetamodel())
          .orElse(viewUri);
      this.viewResourceAccess = new ViewResourceAccessImpl(this.viewUri);

      this.internalView =
          new InternalViewImpl(getStructure(), viewResourceAccess, originResourceAccess,
                               observable != null ? new ViewObserver(observable)
                                                  : DefaultViewObserver.INSTANCE);
      this.internalView.update();

      if (correspondenceResolverFactory != null) {
        this.correspondenceResolver = correspondenceResolverFactory.createCorrespondenceResolver(
            ChangeSpecificationAwareViewType.this, viewResourceAccess);
      } else {
        this.correspondenceResolver = null;
      }
    }

    @Override
    public ResourceAccess getViewResourceAccess() {
      return new ResourceAccess() {
        @Override
        public URI getMetadataModelURI(String... strings) {
          throw new UnsupportedOperationException();
        }

        @Override
        public Resource getModelResource(URI uri) {
          Resource resource = viewResourceAccess
              .getResourceSet()
              .getResource(uri, true);
          if (resource == null) {
            resource = viewResourceAccess
                .getResourceSet()
                .createResource(uri);
          }
          return resource;
        }

        @Override
        public Collection<Resource> getModelResources() {
          return viewResourceAccess
              .getResourceSet()
              .getResources();
        }

        @Override
        public void persistAsRoot(EObject eObject, URI uri) {
          if (!uri
              .fileExtension()
              .equals(getMetamodel().getNsPrefix())) {
            throw new IllegalArgumentException(
                "View roots must be persisted using the view type metamodel's file extension ("
                    + getMetamodel().getNsPrefix() + "), but was " + uri.fileExtension());
          }
          viewResourceAccess.registerRoot(eObject, uri);
        }
      };
    }

    @Override
    public List<EChange<EObject>> fitAndDetermineChanges(ResourceAccess changedOrigin,
                                                         CorrespondenceModelAccess changedCorrespondenceModel,
                                                         List<EChange<EObject>> originChanges) {
      List<EChange<EObject>> viewChanges;

      try (ChangePropagationViewImpl changedView = new ChangePropagationViewImpl(changedOrigin,
                                                                                 changedCorrespondenceModel,
                                                                                 viewUri, null,
                                                                                 null)
      ) {
        viewChanges =
            deriveAndApplyChangesToReach(changedView, getStateBasedChangeResolutionStrategy());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      if (correspondenceResolver != null) {
        correspondenceResolver.onViewFitted();
      }

      return viewChanges;
    }

    @Override
    public CorrespondenceResolver getCorrespondenceResolver() {
      if (correspondenceResolver != null) {
        correspondenceResolver.onResolverUse();
      }
      return correspondenceResolver;
    }

    @Override
    public void commit() {
      internalView.commit();
    }

    private StateBasedChangeResolutionStrategy getStateBasedChangeResolutionStrategy() {
      return new RootPreservingStateBasedChangeResolutionStrategy();
    }

    private List<EChange<EObject>> deriveAndApplyChangesToReach(
        ChangePropagationViewImpl changedView,
        StateBasedChangeResolutionStrategy stateBasedChangeResolutionStrategy) {
      Map<URI, Resource> localResourceMap = getResources();
      Map<URI, Resource> changedResourceMap = changedView.getResources();

      List<URI> uris = Sets
          .union(localResourceMap.keySet(), changedResourceMap.keySet())
          .stream()
          .sorted(Comparator.comparing(URI::toString))
          .toList();
      List<VitruviusChange<HierarchicalId>> changes = new ArrayList<>();

      for (URI uri : uris) {
        Resource localResource = localResourceMap.get(uri);
        Resource changedResource = changedResourceMap.get(uri);
        assert localResource != null || changedResource != null;

        VitruviusChange<HierarchicalId> change;

        if (localResource == null) {
          change = stateBasedChangeResolutionStrategy.getChangeSequenceForCreated(changedResource);
        } else if (changedResource == null) {
          change = stateBasedChangeResolutionStrategy.getChangeSequenceForDeleted(localResource);
        } else {
          change = stateBasedChangeResolutionStrategy.getChangeSequenceBetween(changedResource,
                                                                               localResource);
        }

        if (change.containsConcreteChange()) {
          changes.add(change);
        }
      }

      if (changes.isEmpty()) {
        return List.of();
      }

      VitruviusChange<HierarchicalId> change = VitruviusChangeFactory
          .getInstance()
          .createCompositeChange(changes);

      return VitruviusChangeResolverFactory
          .forHierarchicalIds(viewResourceAccess.getResourceSet())
          .resolveAndApply(change)
          .getEChanges();
    }

    private Map<URI, Resource> getResources() {
      Map<URI, Resource> resources = new HashMap<>();
      for (Resource resource : viewResourceAccess
          .getResourceSet()
          .getResources()) {
        resources.put(resource.getURI(), resource);
      }
      return resources;
    }

    @Override
    public void close() throws Exception {
      if (correspondenceResolver != null) {
        correspondenceResolver.close();
      }

      internalView.close();

      viewResourceAccess.close();
      originResourceAccess.close();
    }

    private static final class ViewObserver extends DefaultViewObserver {
      private final ChangePropagationObservable observable;

      private ViewObserver(ChangePropagationObservable observable) {
        this.observable = observable;
      }

      @Override
      public void originObjectCreated(EObject eObject) {
        observable.notifyObjectCreated(eObject);
      }
    }
  }
}
