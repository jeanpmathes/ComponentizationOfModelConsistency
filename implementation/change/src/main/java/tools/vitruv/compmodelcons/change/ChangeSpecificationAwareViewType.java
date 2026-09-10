package tools.vitruv.compmodelcons.change;

import com.google.common.collect.Sets;
import java.util.ArrayList;
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
import tools.vitruv.change.propagation.ModelRepositorySnapshot;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceObjectViewObjectTranslator;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceObjectViewObjectTranslatorFactory;
import tools.vitruv.compmodelcons.change.correspondence.ViewCorrespondences;
import tools.vitruv.compmodelcons.change.correspondence.impl.ViewCorrespondencesImpl;
import tools.vitruv.compmodelcons.change.impl.ViewAdaptedStateBasedChangeResolutionStrategy;
import tools.vitruv.compmodelcons.change.impl.ViewSnapshot;
import tools.vitruv.compmodelcons.views.impl.DefaultViewObserver;
import tools.vitruv.compmodelcons.views.impl.OperationBasedViewType;
import tools.vitruv.compmodelcons.views.impl.ViewResourceAccessImpl;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;
import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;
import tools.vitruv.compmodelcons.views.internal.impl.InternalViewImpl;
import tools.vitruv.compmodelcons.views.internal.impl.ResourceAccessWrappingOriginResourceAccess;
import tools.vitruv.framework.views.ViewTypeProvider;

/**
 * Abstract base class for view types that can participate in change propagation.
 */
public abstract class ChangeSpecificationAwareViewType extends OperationBasedViewType
    implements ChangePropagatingViewTypeSpecification {
  public ChangeSpecificationAwareViewType(
      String name, List<EPackage> originMetamodels, EPackage viewTypeMetamodel,
      ViewTypeProvider viewTypeProvider) {
    super(name, originMetamodels, viewTypeMetamodel, viewTypeProvider);
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
  public ChangePropagationView createView(
      ResourceAccess resourceAccess, CorrespondenceModelAccess correspondenceModelAccess,
      Function<String, URI> uriFactory, ChangePropagationObservable observable,
      CorrespondenceObjectViewObjectTranslatorFactory correspondenceObjectViewObjectTranslatorFactory) {
    return new ChangePropagationViewImpl(resourceAccess, correspondenceModelAccess,
                                         createUri(uriFactory), observable,
                                         correspondenceObjectViewObjectTranslatorFactory);
  }

  private class ChangePropagationViewImpl implements ChangePropagationView {
    private final OriginResourceAccess originResourceAccess;
    private final ViewResourceAccess viewResourceAccess;
    private final InternalViewImpl internalView;

    private final URI viewUri;

    private final CorrespondenceModelAccess correspondenceModelAccess;
    private final CorrespondenceObjectViewObjectTranslator correspondenceObjectViewObjectTranslator;
    private final ViewCorrespondencesImpl viewCorrespondences;

    public ChangePropagationViewImpl(
        ResourceAccess resourceAccess, CorrespondenceModelAccess correspondenceModelAccess,
        URI viewUri, ChangePropagationObservable observable,
        CorrespondenceObjectViewObjectTranslatorFactory correspondenceObjectViewObjectTranslatorFactory) {
      this.originResourceAccess = new ResourceAccessWrappingOriginResourceAccess(resourceAccess,
                                                                                 correspondenceModelAccess.getResource());
      this.viewUri = this.originResourceAccess.getViewUriHint(getOriginMetamodels(), getMetamodel())
                         .orElse(viewUri);
      this.viewResourceAccess =
          new ViewResourceAccessImpl(this.viewUri, resourceAccess::getMetadataModelURI);

      this.internalView =
          new InternalViewImpl(getStructure(), viewResourceAccess, originResourceAccess,
                               observable != null ? new ViewObserver(observable)
                                                  : DefaultViewObserver.INSTANCE);
      this.internalView.update();

      this.correspondenceModelAccess = correspondenceModelAccess;
      this.viewCorrespondences = new ViewCorrespondencesImpl(getViewTypeMetamodelDescriptor(),
                                                             getOriginMetamodelDescriptor(),
                                                             internalView.getCorrespondences());

      if (correspondenceObjectViewObjectTranslatorFactory != null) {
        this.correspondenceObjectViewObjectTranslator =
            correspondenceObjectViewObjectTranslatorFactory.createCorrespondenceResolver(
                ChangeSpecificationAwareViewType.this, viewResourceAccess);
      } else {
        this.correspondenceObjectViewObjectTranslator = null;
      }
    }

    @Override
    public ResourceAccess getViewResourceAccess() {
      return viewResourceAccess;
    }

    @Override
    public ModelRepositorySnapshot createSnapshot() {
      return new ViewSnapshot(viewResourceAccess,
                              correspondenceModelAccess.getCorrespondenceModel());
    }

    @Override
    public List<EChange<EObject>> fitAndDetermineChanges(
        ResourceAccess changedOrigin, CorrespondenceModelAccess changedCorrespondenceModel,
        List<EChange<EObject>> originChanges,
        Function<EObject, EObject> unchangedToChanged) {
      List<EChange<EObject>> viewChanges;

      try (ChangePropagationViewImpl changedView = new ChangePropagationViewImpl(changedOrigin,
                                                                                 changedCorrespondenceModel,
                                                                                 viewUri, null,
                                                                                 null)
      ) {
        StateBasedChangeResolutionStrategy stateBasedChangeResolutionStrategy =
            getStateBasedChangeResolutionStrategy(this, changedView, unchangedToChanged);
        viewChanges =
            deriveAndApplyChangesToReach(changedView, stateBasedChangeResolutionStrategy);
        internalView.mapAndApplyCorrespondences(changedView.internalView.getCorrespondences(),
                                                unchangedToChanged);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      if (correspondenceObjectViewObjectTranslator != null) {
        correspondenceObjectViewObjectTranslator.onViewFitted();
      }

      return viewChanges;
    }

    @Override
    public CorrespondenceObjectViewObjectTranslator getCorrespondenceResolver() {
      if (correspondenceObjectViewObjectTranslator != null) {
        correspondenceObjectViewObjectTranslator.onResolverUse();
      }
      return correspondenceObjectViewObjectTranslator;
    }

    @Override
    public ViewCorrespondences getCorrespondences() {
      return viewCorrespondences;
    }

    @Override
    public void commit() {
      internalView.commit();
    }

    private static StateBasedChangeResolutionStrategy getStateBasedChangeResolutionStrategy(
        ChangePropagationViewImpl unchangedView,
        ChangePropagationViewImpl changedView,
        Function<EObject, EObject> unchangedToChanged) {
      return new ViewAdaptedStateBasedChangeResolutionStrategy(viewObject -> {
        Resource resource = viewObject.eResource();
        if (resource == null || resource.getResourceSet() == null) {
          return null;
        }
        if (resource.getResourceSet() == changedView.viewResourceAccess.getResourceSet()) {
          return changedView.internalView.getCorrespondences()
                     .getCorrespondingOriginObjectsForViewObject(viewObject);
        } else {
          // Change Resolution creates a copy of the unchanged side, so we have to map back.

          Resource originalResource = unchangedView.viewResourceAccess.getResourceSet()
                                          .getResource(resource.getURI(), false);
          if (originalResource == null) {
            return null;
          }

          EObject originalViewObject =
              originalResource.getEObject(resource.getURIFragment(viewObject));
          if (originalViewObject == null) {
            return null;
          }

          List<EObject> originObjects = unchangedView.internalView.getCorrespondences()
                                            .getCorrespondingOriginObjectsForViewObject(
                                                originalViewObject);
          if (originObjects == null) {
            return null;
          }

          return originObjects.stream().map(unchangedToChanged).toList();
        }
      });
    }

    private List<EChange<EObject>> deriveAndApplyChangesToReach(
        ChangePropagationViewImpl changedView,
        StateBasedChangeResolutionStrategy stateBasedChangeResolutionStrategy) {
      Map<URI, Resource> localResourceMap = getResources();
      Map<URI, Resource> changedResourceMap = changedView.getResources();

      List<URI> uris = Sets.union(localResourceMap.keySet(), changedResourceMap.keySet())
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

      VitruviusChange<HierarchicalId> change =
          VitruviusChangeFactory.getInstance().createCompositeChange(changes);

      return VitruviusChangeResolverFactory.forHierarchicalIds(viewResourceAccess.getResourceSet())
                 .resolveAndApply(change)
                 .getEChanges();
    }

    private Map<URI, Resource> getResources() {
      Map<URI, Resource> resources = new HashMap<>();
      for (Resource resource : viewResourceAccess.getResourceSet().getResources()) {
        resources.put(resource.getURI(), resource);
      }
      return resources;
    }

    @Override
    public void close() throws Exception {
      if (correspondenceObjectViewObjectTranslator != null) {
        correspondenceObjectViewObjectTranslator.close();
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
