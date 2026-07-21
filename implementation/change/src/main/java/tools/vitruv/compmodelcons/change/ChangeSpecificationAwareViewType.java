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
import tools.vitruv.change.propagation.ModelSnapshot;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.viewid.model.ViewId;
import tools.vitruv.compmodelcons.change.viewid.model.ViewIdModel;
import tools.vitruv.compmodelcons.change.viewid.model.ViewIdModelFactory;
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

public abstract class ChangeSpecificationAwareViewType extends OperationBasedViewType implements ChangePropagationViewTypeSpecification {
    public ChangeSpecificationAwareViewType(String name, List<EPackage> originMetamodels, EPackage viewTypeMetamodel) {
        super(name, originMetamodels, viewTypeMetamodel);
    }

    @Override
    public List<MetamodelDescriptor> getOriginMetamodelDescriptors() {
        return getOriginMetamodels().stream().map(MetamodelDescriptor::of).toList();
    }

    @Override
    public MetamodelDescriptor getViewTypeMetamodelDescriptor() {
        return MetamodelDescriptor.of(getMetamodel());
    }

    @Override
    public ChangePropagationView createView(int originMetamodelIndex, ResourceAccess resourceAccess, Function<String, URI> uriFactory, CorrespondenceResolvingContext correspondenceContext) {
        return new ChangePropagationViewImpl(originMetamodelIndex, resourceAccess, createUri(uriFactory), Optional.of(correspondenceContext));
    }

    private URI getIdModelURI(ResourceAccess resourceAccess) {
        return resourceAccess.getMetadataModelURI("views", String.format("%s.viewid", getName()));
    }

    private class ChangePropagationViewImpl implements ChangePropagationView {
        private final int originMetamodelIndex;
        private final ResourceAccess resourceAccess;
        private final OriginResourceAccess originResourceAccess;
        private final ViewResourceAccess viewResourceAccess;
        private final InternalViewImpl internalView;
        private final URI viewUri;
        private final CorrespondenceResolverImpl correspondenceResolver;

        public ChangePropagationViewImpl(int originMetamodelIndex, ResourceAccess resourceAccess, URI viewUri, Optional<CorrespondenceResolvingContext> correspondenceContext) {
            this.originMetamodelIndex = originMetamodelIndex;
            this.resourceAccess = resourceAccess;
            this.originResourceAccess = new ResourceAccessWrappingOriginResourceAccess(resourceAccess);
            this.viewUri = this.originResourceAccess.getViewUriHint(getOriginMetamodels().get(originMetamodelIndex), getMetamodel()).orElse(viewUri);
            this.viewResourceAccess = new ViewResourceAccessImpl(this.viewUri);
            this.internalView = new InternalViewImpl(getStructure(), viewResourceAccess, originResourceAccess);

            internalView.update();

            if (correspondenceContext.isPresent()) {
                ViewIdModel viewIdModel = loadViewIdModel(correspondenceContext.get());
                this.correspondenceResolver = new CorrespondenceResolverImpl(viewIdModel, createViewIdMapping(viewIdModel));
            } else {
                this.correspondenceResolver = null;
            }
        }

        private ViewIdModel loadViewIdModel(CorrespondenceResolvingContext correspondenceContext) {
            Resource resource = correspondenceContext.resourceAccess().getModelResource(getIdModelURI(correspondenceContext.resourceAccess()));

            if (resource.getContents().isEmpty()) {
                resource.getContents().add(ViewIdModelFactory.eINSTANCE.createViewIdModel());
            }

            return (ViewIdModel) resource.getContents().getFirst();
        }

        private BiMap<EObject, ViewId> createViewIdMapping(ViewIdModel viewIdModel) {
            BiMap<EObject, ViewId> viewObjectToViewId = HashBiMap.create();

            HierarchicalIdResolver hierarchicalIdResolver = HierarchicalIdResolver.create(viewResourceAccess.getResourceSet());
            for (ViewId viewId : viewIdModel.getIds()) {
                try {
                    viewObjectToViewId.put(hierarchicalIdResolver.getEObject(new HierarchicalId(viewId.getHierarchicalId())), viewId);
                } catch (IllegalStateException e) {
                    throw new RuntimeException("Could not resolve view id " + viewId.getHierarchicalId() + ", working in resource set " + viewResourceAccess.getResourceSet(), e);
                }
            }

            return viewObjectToViewId;
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
                        throw new IllegalArgumentException("View roots must be persisted using the view type metamodel's file extension (" + getMetamodel().getNsPrefix() + "), but was " + uri.fileExtension());
                    }
                    viewResourceAccess.registerRoot(eObject, uri);
                }
            };
        }

        @Override
        public CorrespondenceResolver getCorrespondenceResolver() {
            return correspondenceResolver;
        }

        @Override
        public List<EChange<EObject>> fitAndDetermineChanges(ResourceAccess changedOrigin, List<EChange<EObject>> originChanges, ChangeDeterminationMode changeDeterminationMode) {
            switch (changeDeterminationMode) {
                case CHANGE_DERIVATION -> {
                    StateBasedChangeResolutionStrategy stateBasedChangeResolutionStrategy = getStateBasedChangeResolutionStrategy();

                    List<EChange<EObject>> result;
                    try (ChangePropagationViewImpl changedView = new ChangePropagationViewImpl(originMetamodelIndex, changedOrigin, viewUri, Optional.empty())) {
                        result = deriveAndApplyChangesToReach(changedView, stateBasedChangeResolutionStrategy);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return result;
                }
                case UPDATING_GET -> {
                    if (!(resourceAccess instanceof ModelSnapshot snapshot)) {
                        throw new IllegalStateException("Cannot use updating get with a non-snapshot resource access");
                    }

                    SnapshotChangeApplier applier = new SnapshotChangeApplier(snapshot.copy());
                    List<EChange<EObject>> viewChanges = new ArrayList<>();
                    for (EChange<EObject> repositoryOriginChange : originChanges) {
                        EChange<EObject> snapshotChange = applier.apply(repositoryOriginChange);
                        viewChanges.addAll(internalView.updateAndTranslateChange(snapshotChange));
                    }
                    return viewChanges;
                }
            }

            throw new UnsupportedOperationException("Unsupported change determination mode: " + changeDeterminationMode);
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

            List<URI> uris = Sets.union(localResourceMap.keySet(), changedResourceMap.keySet()).stream().sorted(Comparator.comparing(URI::toString)).toList();
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

            VitruviusChange<HierarchicalId> change = VitruviusChangeFactory.getInstance().createCompositeChange(changes);

            return VitruviusChangeResolverFactory.forHierarchicalIds(viewResourceAccess.getResourceSet()).resolveAndApply(change).getEChanges();
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

        private class CorrespondenceResolverImpl implements CorrespondenceResolver {
            private final ViewIdModel viewIdModel;
            private final BiMap<EObject, ViewId> viewObjectToViewId;

            private CorrespondenceResolverImpl(ViewIdModel viewIdModel, BiMap<EObject, ViewId> viewObjectToViewId) {
                this.viewIdModel = viewIdModel;
                this.viewObjectToViewId = viewObjectToViewId;
            }

            @Override
            public boolean canResolveViewEObject(EObject viewObject) {
                return viewObject.eClass().getEPackage().equals(getMetamodel());
            }

            @Override
            public boolean canResolveCorrespondenceEObject(EObject correspondenceObject) {
                return correspondenceObject.eClass().getEPackage().equals(ViewIdModelFactory.eINSTANCE.getViewIdModelPackage())
                        && correspondenceObject instanceof ViewId viewId
                        && viewObjectToViewId.inverse().containsKey(viewId);
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
                if (viewObjectToViewId == null || viewIdModel == null) {
                    return;
                }
                HierarchicalIdResolver hierarchicalIdResolver = HierarchicalIdResolver.create(viewResourceAccess.getResourceSet());
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
