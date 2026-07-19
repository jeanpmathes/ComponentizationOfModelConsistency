package tools.vitruv.compmodelcons.change.impl;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.CorrespondenceModelView;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.compmodelcons.change.AbstractChangePropagationSpecificationWrappingStrategy;
import tools.vitruv.compmodelcons.change.CorrespondenceResolver;
import tools.vitruv.compmodelcons.change.ViewChangePropagationContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class RemoteChangePropagationSpecificationWrappingStrategy extends AbstractChangePropagationSpecificationWrappingStrategy {
    public RemoteChangePropagationSpecificationWrappingStrategy(ChangePropagationSpecification specification) {
        super(specification);
    }

    @Override
    public EditableCorrespondenceModelView<Correspondence> wrapCorrespondenceModel(EditableCorrespondenceModelView<Correspondence> correspondenceModel, ViewChangePropagationContext context) {
        return new RemoteEditableCorrespondenceModelViewImpl<>(correspondenceModel, context.sourceView().getCorrespondenceResolver(), context.targetView().getCorrespondenceResolver());
    }

    private static class RemoteCorrespondenceModelViewImpl<C extends Correspondence> implements CorrespondenceModelView<C> {
        protected final CorrespondenceModelView<C> inner;
        protected final CorrespondenceResolver sourceResolver;
        protected final CorrespondenceResolver targetResolver;

        private RemoteCorrespondenceModelViewImpl(CorrespondenceModelView<C> inner, CorrespondenceResolver sourceResolver, CorrespondenceResolver targetResolver) {
            this.inner = inner;
            this.sourceResolver = sourceResolver;
            this.targetResolver = targetResolver;
        }

        @Override
        public boolean hasCorrespondences(List<EObject> eObjects) {
            List<EObject> correspondenceObjects = getCorrespondenceEObjects(eObjects, false);
            return correspondenceObjects != null && inner.hasCorrespondences(correspondenceObjects);
        }

        @Override
        public Set<List<EObject>> getCorrespondingEObjects(List<EObject> eObjects) {
            List<EObject> correspondenceObjects = getCorrespondenceEObjects(eObjects, false);
            if (correspondenceObjects == null) {
                return Set.of();
            }
            return getViewEObjects(inner.getCorrespondingEObjects(correspondenceObjects));
        }

        @Override
        public Set<List<EObject>> getCorrespondingEObjects(List<EObject> objects, String tag) {
            List<EObject> correspondenceObjects = getCorrespondenceEObjects(objects, false);
            if (correspondenceObjects == null) {
                return Set.of();
            }
            return getViewEObjects(inner.getCorrespondingEObjects(correspondenceObjects, tag));
        }

        @Override
        public <V extends C> CorrespondenceModelView<V> getView(Class<V> correspondenceType) {
            return new RemoteCorrespondenceModelViewImpl<>(inner.getView(correspondenceType), sourceResolver, targetResolver);
        }

        protected List<EObject> getCorrespondenceEObjects(List<EObject> eObjects, boolean createIfNotExist) {
            List<EObject> correspondenceEObjects = new ArrayList<>(eObjects.size());
            for (EObject eObject : eObjects) {
                EObject correspondenceObject = getCorrespondenceEObject(eObject, createIfNotExist);
                if (correspondenceObject == null) {
                    return null;
                }
                correspondenceEObjects.add(correspondenceObject);
            }
            return correspondenceEObjects;
        }

        private EObject getCorrespondenceEObject(EObject viewEObject, boolean createIfNotExist) {
            if (sourceResolver.canResolveViewEObject(viewEObject)) {
                return sourceResolver.getCorrespondenceEObject(viewEObject, createIfNotExist);
            }
            if (targetResolver.canResolveViewEObject(viewEObject)) {
                return targetResolver.getCorrespondenceEObject(viewEObject, createIfNotExist);
            }
            return null;
        }

        protected Set<List<EObject>> getViewEObjects(Set<List<EObject>> eObjects) {
            Set<List<EObject>> viewEObjects = new HashSet<>(eObjects.size());
            for (List<EObject> eObject : eObjects) {
                viewEObjects.add(eObject.stream().map(this::getViewEObject).toList());
            }
            return viewEObjects;
        }

        private EObject getViewEObject(EObject correspondenceEObject) {
            if (sourceResolver.canResolveCorrespondenceEObject(correspondenceEObject)) {
                return sourceResolver.getViewEObject(correspondenceEObject);
            }
            if (targetResolver.canResolveCorrespondenceEObject(correspondenceEObject)) {
                return targetResolver.getViewEObject(correspondenceEObject);
            }
            return null;
        }
    }

    private static class RemoteEditableCorrespondenceModelViewImpl<C extends Correspondence> extends RemoteCorrespondenceModelViewImpl<C> implements EditableCorrespondenceModelView<C> {
        private final EditableCorrespondenceModelView<C> editableInner;

        private RemoteEditableCorrespondenceModelViewImpl(EditableCorrespondenceModelView<C> inner, CorrespondenceResolver sourceResolver, CorrespondenceResolver targetResolver) {
            super(inner, sourceResolver, targetResolver);
            this.editableInner = inner;
        }

        @Override
        public C addCorrespondenceBetween(List<EObject> first, List<EObject> second, String tag) {
            return editableInner.addCorrespondenceBetween(getCorrespondenceEObjects(first, true), getCorrespondenceEObjects(second, true), tag);
        }

        @Override
        public Set<C> removeCorrespondencesBetween(List<EObject> first, List<EObject> second, String tag) {
            List<EObject> firstCorrespondenceObjects = getCorrespondenceEObjects(first, false);
            List<EObject> secondCorrespondenceObjects = getCorrespondenceEObjects(second, false);

            if (firstCorrespondenceObjects == null || secondCorrespondenceObjects == null) {
                return Set.of();
            }

            return editableInner.removeCorrespondencesBetween(firstCorrespondenceObjects, secondCorrespondenceObjects, tag);
        }

        @Override
        public <V extends C> EditableCorrespondenceModelView<V> getEditableView(Class<V> correspondenceType, Supplier<V> supplier) {
            return new RemoteEditableCorrespondenceModelViewImpl<>(editableInner.getEditableView(correspondenceType, supplier), sourceResolver, targetResolver);
        }
    }
}
