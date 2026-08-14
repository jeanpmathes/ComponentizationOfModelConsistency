package tools.vitruv.compmodelcons.change;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.atomic.hid.internal.HierarchicalIdResolver;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.description.VitruviusChangeFactory;
import tools.vitruv.change.composite.description.VitruviusChangeResolverFactory;
import tools.vitruv.change.propagation.ChangePropagationObservable;
import tools.vitruv.change.propagation.ModelSnapshot;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.viewid.model.ViewId;
import tools.vitruv.compmodelcons.change.viewid.model.ViewIdModel;
import tools.vitruv.compmodelcons.change.viewid.model.ViewIdModelFactory;
import tools.vitruv.compmodelcons.views.impl.DefaultViewObserver;
import tools.vitruv.compmodelcons.views.impl.OperationBasedViewType;
import tools.vitruv.compmodelcons.views.impl.ViewResourceAccessImpl;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;
import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;
import tools.vitruv.compmodelcons.views.internal.impl.InternalViewImpl;
import tools.vitruv.compmodelcons.views.internal.impl.ResourceAccessWrappingOriginResourceAccess;
import tools.vitruv.framework.views.changederivation.DefaultStateBasedChangeResolutionStrategy;
import tools.vitruv.framework.views.changederivation.StateBasedChangeResolutionStrategy;

import java.util.*;
import java.util.function.Function;

public abstract class ChangeSpecificationAwareViewType extends OperationBasedViewType implements ChangePropagatingViewTypeSpecification {
    public ChangeSpecificationAwareViewType(String name, List<EPackage> originMetamodels, EPackage viewTypeMetamodel) {
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
    public ChangePropagationView createView(ResourceAccess resourceAccess, CorrespondenceModelAccess correspondenceModelAccess, Function<String, URI> uriFactory, ChangePropagationObservable observable, CorrespondenceResolvingContext correspondenceContext) {
        return new ChangePropagationViewImpl(resourceAccess, correspondenceModelAccess, createUri(uriFactory), observable, Optional.of(correspondenceContext));
    }

    private URI getIdModelURI(ResourceAccess resourceAccess) {
        return resourceAccess.getMetadataModelURI("views", String.format("%s.viewid", getName()));
    }

    private class ChangePropagationViewImpl implements ChangePropagationView {
        private final ResourceAccess resourceAccess;
        private final OriginResourceAccess originResourceAccess;
        private final ViewResourceAccess viewResourceAccess;
        private final InternalViewImpl internalView;
        private final URI viewUri;
        private final CorrespondenceResolverImpl correspondenceResolver;

        public ChangePropagationViewImpl(ResourceAccess resourceAccess, CorrespondenceModelAccess correspondenceModelAccess, URI viewUri, ChangePropagationObservable observable, Optional<CorrespondenceResolvingContext> correspondenceContext) {
            this.resourceAccess = resourceAccess;
            this.originResourceAccess = new ResourceAccessWrappingOriginResourceAccess(resourceAccess, correspondenceModelAccess.getResource());
            this.viewUri = this.originResourceAccess.getViewUriHint(getOriginMetamodels(), getMetamodel())
                                                    .orElse(viewUri);
            this.viewResourceAccess = new ViewResourceAccessImpl(this.viewUri);

            this.internalView = new InternalViewImpl(getStructure(), viewResourceAccess, originResourceAccess,
                                                     observable != null ? new ViewObserver(observable)
                                                                        : DefaultViewObserver.INSTANCE);

            this.internalView.update();

            if (correspondenceContext.isPresent()) {
                ViewIdModel viewIdModel = loadViewIdModel(correspondenceContext.get());
                HierarchicalIdResolver hierarchicalIdResolver = HierarchicalIdResolver.create(viewResourceAccess.getResourceSet());
                this.correspondenceResolver = new CorrespondenceResolverImpl(viewIdModel, hierarchicalIdResolver);
            } else {
                this.correspondenceResolver = null;
            }
        }

        private ViewIdModel loadViewIdModel(CorrespondenceResolvingContext correspondenceContext) {
            Resource resource = correspondenceContext.resourceAccess()
                                                     .getModelResource(getIdModelURI(correspondenceContext.resourceAccess()));

            if (resource.getContents().isEmpty()) {
                resource.getContents().add(ViewIdModelFactory.eINSTANCE.createViewIdModel());
            }

            return (ViewIdModel) resource.getContents().getFirst();
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
                    Resource resource = viewResourceAccess.getResourceSet().getResource(uri, true);
                    if (resource == null) {
                        resource = viewResourceAccess.getResourceSet().createResource(uri);
                    }
                    return resource;
                }

                @Override
                public Collection<Resource> getModelResources() {
                    return viewResourceAccess.getResourceSet().getResources();
                }

                @Override
                public void persistAsRoot(EObject eObject, URI uri) {
                    if (!uri.fileExtension().equals(getMetamodel().getNsPrefix())) {
                        throw new IllegalArgumentException(
                                "View roots must be persisted using the view type metamodel's file extension (" +
                                        getMetamodel().getNsPrefix() + "), but was " + uri.fileExtension());
                    }
                    viewResourceAccess.registerRoot(eObject, uri);
                }
            };
        }

        @Override
        public List<EChange<EObject>> fitAndDetermineChanges(ResourceAccess changedOrigin, CorrespondenceModelAccess changedCorrespondenceModel, List<EChange<EObject>> originChanges, ChangeDeterminationMode changeDeterminationMode) {
            List<EChange<EObject>> viewChanges;

            switch (changeDeterminationMode) {
                case CHANGE_DERIVATION -> {
                    StateBasedChangeResolutionStrategy stateBasedChangeResolutionStrategy = getStateBasedChangeResolutionStrategy();

                    try (ChangePropagationViewImpl changedView = new ChangePropagationViewImpl(changedOrigin, changedCorrespondenceModel, viewUri, null, Optional.empty())) {
                        viewChanges = deriveAndApplyChangesToReach(changedView, stateBasedChangeResolutionStrategy);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                case UPDATING_GET -> {
                    if (!(resourceAccess instanceof ModelSnapshot snapshot)) {
                        throw new IllegalStateException("Cannot use updating get with a non-snapshot resource access");
                    }

                    SnapshotChangeApplier applier = new SnapshotChangeApplier(snapshot.copy());
                    viewChanges = new ArrayList<>();
                    for (EChange<EObject> repositoryOriginChange : originChanges) {
                        EChange<EObject> snapshotChange = applier.apply(repositoryOriginChange);
                        viewChanges.addAll(internalView.updateAndTranslateChange(snapshotChange));
                    }
                }
                default -> throw new UnsupportedOperationException(
                        "Unsupported change determination mode: " + changeDeterminationMode);
            }

            if (correspondenceResolver != null) {
                correspondenceResolver.resolveUnresolvedViewIds();
            }

            return viewChanges;
        }

        @Override
        public CorrespondenceResolver getCorrespondenceResolver() {
            correspondenceResolver.ensureNoUnresolvedViewIds();
            return correspondenceResolver;
        }

        @Override
        public void commit() {
            internalView.commit();
        }

        private StateBasedChangeResolutionStrategy getStateBasedChangeResolutionStrategy() {
            return new DefaultStateBasedChangeResolutionStrategy(UseIdentifiers.NEVER);
        }

        private List<EChange<EObject>> deriveAndApplyChangesToReach(ChangePropagationViewImpl changedView, StateBasedChangeResolutionStrategy stateBasedChangeResolutionStrategy) {
            Map<URI, Resource> localResourceMap = getResources();
            Map<URI, Resource> changedResourceMap = changedView.getResources();

            List<URI> uris = Sets.union(localResourceMap.keySet(), changedResourceMap.keySet()).stream()
                                 .sorted(Comparator.comparing(URI::toString)).toList();
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
                    change = stateBasedChangeResolutionStrategy.getChangeSequenceBetween(changedResource, localResource);
                }

                if (change.containsConcreteChange()) {
                    changes.add(change);
                }
            }

            if (changes.isEmpty()) {
                return List.of();
            }

            VitruviusChange<HierarchicalId> change = VitruviusChangeFactory.getInstance()
                                                                           .createCompositeChange(changes);

            return VitruviusChangeResolverFactory.forHierarchicalIds(viewResourceAccess.getResourceSet())
                                                 .resolveAndApply(change).getEChanges();
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

        private class CorrespondenceResolverImpl implements CorrespondenceResolver {
            private final ViewIdModel viewIdModel;
            private final HierarchicalIdResolver hierarchicalIdResolver;

            private final BiMap<EObject, ViewId> viewObjectToViewId = HashBiMap.create();
            private final Map<HierarchicalId, ViewId> unresolvedHierarchicalIds = new HashMap<>();

            private CorrespondenceResolverImpl(ViewIdModel viewIdModel, HierarchicalIdResolver hierarchicalIdResolver) {
                this.viewIdModel = viewIdModel;
                this.hierarchicalIdResolver = hierarchicalIdResolver;

                initializeMap();
            }

            private void initializeMap() {
                Set<HierarchicalId> hierarchicalIds = new HashSet<>();

                for (ViewId viewId : viewIdModel.getIds()) {
                    HierarchicalId hierarchicalId = new HierarchicalId(viewId.getHierarchicalId());
                    if (!hierarchicalIds.add(hierarchicalId)) {
                        throw new IllegalStateException(
                                "Duplicate hierarchical ID found in view ID model: " + hierarchicalId);
                    }
                    try {
                        EObject viewObject = hierarchicalIdResolver.getEObject(hierarchicalId);
                        viewObjectToViewId.put(viewObject, viewId);
                    } catch (IllegalStateException e) {
                        unresolvedHierarchicalIds.put(hierarchicalId, viewId);
                    }
                }
            }

            public void resolveUnresolvedViewIds() {
                Iterator<Map.Entry<HierarchicalId, ViewId>> iterator = unresolvedHierarchicalIds.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<HierarchicalId, ViewId> entry = iterator.next();
                    HierarchicalId hierarchicalId = entry.getKey();
                    ViewId viewId = entry.getValue();
                    try {
                        EObject viewObject = hierarchicalIdResolver.getEObject(hierarchicalId);
                        viewObjectToViewId.put(viewObject, viewId);
                        iterator.remove();
                    } catch (IllegalStateException e) {
                        // The view ID remains unresolved.
                    }
                }
            }

            public void ensureNoUnresolvedViewIds() {
                if (!unresolvedHierarchicalIds.isEmpty()) {
                    throw new IllegalStateException(
                            "The following view IDs could not be resolved: " + unresolvedHierarchicalIds.keySet());
                }
            }

            @Override
            public boolean canResolveViewEObject(EObject viewObject) {
                return viewObject.eClass().getEPackage().equals(getMetamodel());
            }

            @Override
            public boolean canResolveCorrespondenceEObject(EObject correspondenceObject) {
                return correspondenceObject.eClass().getEPackage()
                                           .equals(ViewIdModelFactory.eINSTANCE.getViewIdModelPackage()) &&
                        correspondenceObject instanceof ViewId viewId &&
                        viewObjectToViewId.inverse().containsKey(viewId);
            }

            @Override
            public EObject getViewEObject(EObject correspondenceEObject) {
                return viewObjectToViewId.inverse().get((ViewId) correspondenceEObject);
            }

            @Override
            public EObject getCorrespondenceEObject(EObject viewEObject, boolean createIfNotExist) {
                ViewId existingViewId = viewObjectToViewId.get(viewEObject);
                if (existingViewId != null || !createIfNotExist) {
                    return existingViewId;
                }
                ViewId newViewId = ViewIdModelFactory.eINSTANCE.createViewId();
                viewObjectToViewId.put(viewEObject, newViewId);
                viewIdModel.getIds().add(newViewId);
                return newViewId;
            }

            @Override
            public void close() {
                for (ViewId viewId : List.copyOf(viewIdModel.getIds())) {
                    EObject viewObject = viewObjectToViewId.inverse().get(viewId);
                    if (viewObject.eResource() == null || viewObject.eResource().getResourceSet() == null) {
                        viewIdModel.getIds().remove(viewId);
                    } else {
                        viewId.setHierarchicalId(hierarchicalIdResolver.getAndUpdateId(viewObject).getId());
                    }
                }
            }
        }
    }
}
