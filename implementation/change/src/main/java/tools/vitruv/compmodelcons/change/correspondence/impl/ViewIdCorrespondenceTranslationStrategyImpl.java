package tools.vitruv.compmodelcons.change.correspondence.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.atomic.hid.internal.HierarchicalIdResolver;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.ViewChangePropagationContext;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceResolver;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceResolverFactory;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceTranslationStrategy;
import tools.vitruv.compmodelcons.change.viewid.model.ViewId;
import tools.vitruv.compmodelcons.change.viewid.model.ViewIdModel;
import tools.vitruv.compmodelcons.change.viewid.model.ViewIdModelFactory;
import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;
import tools.vitruv.framework.views.ViewType;

public class ViewIdCorrespondenceTranslationStrategyImpl
    implements CorrespondenceTranslationStrategy {

  @Override
  public CorrespondenceResolverFactory createCorrespondenceResolverFactory(
      ResourceAccess resourceAccess) {
    return new RemoteCorrespondenceResolverFactoryImpl(resourceAccess);
  }

  @Override
  public EditableCorrespondenceModelView<Correspondence> createTranslatedCorrespondenceModelView(
      EditableCorrespondenceModelView<Correspondence> inner, ViewChangePropagationContext context) {
    return new TranslatingEditableCorrespondenceModelViewImpl<>(inner,
                                                                context
                                                                    .sourceView()
                                                                    .getCorrespondenceResolver(),
                                                                context
                                                                    .targetView()
                                                                    .getCorrespondenceResolver());
  }

  private record RemoteCorrespondenceResolverFactoryImpl(ResourceAccess actualResourceAccess)
      implements CorrespondenceResolverFactory {

    private ViewIdModel loadViewIdModel(ViewType<?> viewType, ResourceAccess actualResourceAccess) {
      Resource resource =
          actualResourceAccess.getModelResource(getIdModelURI(viewType, actualResourceAccess));

      if (resource
          .getContents()
          .isEmpty()) {
        resource
            .getContents()
            .add(ViewIdModelFactory.eINSTANCE.createViewIdModel());
      }

      return (ViewIdModel) resource
          .getContents()
          .getFirst();
    }

    private URI getIdModelURI(ViewType<?> viewType, ResourceAccess actualResourceAccess) {
      return actualResourceAccess.getMetadataModelURI("views", String.format("%s.viewid",
                                                                             viewType.getName()));
    }

    @Override
    public CorrespondenceResolver createCorrespondenceResolver(ViewType<?> viewType,
                                                               ViewResourceAccess viewResourceAccess) {
      return new RemoteCorrespondenceResolverImpl(loadViewIdModel(viewType, actualResourceAccess),
                                                  viewType.getMetamodel(),
                                                  HierarchicalIdResolver.create(
                                                      viewResourceAccess.getResourceSet()));
    }
  }

  private static class RemoteCorrespondenceResolverImpl implements CorrespondenceResolver {
    private final ViewIdModel viewIdModel;
    private final EPackage metamodel;
    private final HierarchicalIdResolver hierarchicalIdResolver;

    private final BiMap<EObject, ViewId> viewObjectToViewId = HashBiMap.create();
    private final Map<HierarchicalId, ViewId> unresolvedHierarchicalIds = new HashMap<>();

    private RemoteCorrespondenceResolverImpl(ViewIdModel viewIdModel, EPackage metamodel,
                                             HierarchicalIdResolver hierarchicalIdResolver) {
      this.viewIdModel = viewIdModel;
      this.metamodel = metamodel;
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

    private void resolveUnresolvedViewIds() {
      Iterator<Map.Entry<HierarchicalId, ViewId>> iterator = unresolvedHierarchicalIds
          .entrySet()
          .iterator();
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

    private void ensureNoUnresolvedViewIds() {
      if (!unresolvedHierarchicalIds.isEmpty()) {
        throw new IllegalStateException(
            "The following view IDs could not be resolved: " + unresolvedHierarchicalIds.keySet());
      }
    }

    @Override
    public boolean canResolveViewEObject(EObject viewObject) {
      return viewObject
          .eClass()
          .getEPackage()
          .equals(metamodel);
    }

    @Override
    public boolean canResolveCorrespondenceEObject(EObject correspondenceObject) {
      return correspondenceObject
          .eClass()
          .getEPackage()
          .equals(ViewIdModelFactory.eINSTANCE.getViewIdModelPackage())
          && correspondenceObject instanceof ViewId viewId && viewObjectToViewId
          .inverse()
          .containsKey(viewId);
    }

    @Override
    public EObject getViewEObject(EObject correspondenceEObject) {
      return viewObjectToViewId
          .inverse()
          .get((ViewId) correspondenceEObject);
    }

    @Override
    public EObject getCorrespondenceEObject(EObject viewEObject, boolean createIfNotExist) {
      ViewId existingViewId = viewObjectToViewId.get(viewEObject);
      if (existingViewId != null || !createIfNotExist) {
        return existingViewId;
      }
      ViewId newViewId = ViewIdModelFactory.eINSTANCE.createViewId();
      viewObjectToViewId.put(viewEObject, newViewId);
      viewIdModel
          .getIds()
          .add(newViewId);
      return newViewId;
    }

    @Override
    public void onViewFitted() {
      resolveUnresolvedViewIds();
    }

    @Override
    public void onResolverUse() {
      ensureNoUnresolvedViewIds();
    }

    @Override
    public void close() {
      for (ViewId viewId : List.copyOf(viewIdModel.getIds())) {
        EObject viewObject = viewObjectToViewId
            .inverse()
            .get(viewId);
        if (viewObject.eResource() == null || viewObject
            .eResource()
            .getResourceSet() == null) {
          viewIdModel
              .getIds()
              .remove(viewId);
        } else {
          viewId.setHierarchicalId(hierarchicalIdResolver
                                       .getAndUpdateId(viewObject)
                                       .getId());
        }
      }
    }
  }
}
