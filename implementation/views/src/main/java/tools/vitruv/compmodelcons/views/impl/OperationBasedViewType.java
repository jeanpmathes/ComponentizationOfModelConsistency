package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.compmodelcons.views.internal.impl.InternalViewImpl;
import tools.vitruv.compmodelcons.views.internal.impl.ViewWrappingOriginResourceAccessImpl;
import tools.vitruv.compmodelcons.views.operations.Root;
import tools.vitruv.framework.views.*;
import tools.vitruv.framework.views.changederivation.StateBasedChangeResolutionStrategy;
import tools.vitruv.framework.views.impl.AbstractViewType;
import tools.vitruv.framework.views.impl.IdentityMappingViewType;
import tools.vitruv.framework.views.impl.ModifiableView;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public abstract class OperationBasedViewType extends AbstractViewType<AllSelector, HierarchicalId> {
    private final List<EPackage> originMetamodels;
    private final IdentityMappingViewType sourceModelsViewType;

    private Root structure;

    public OperationBasedViewType(String name, List<EPackage> originMetamodels, EPackage viewTypeMetamodel) {
        super(name, viewTypeMetamodel);

        this.originMetamodels = List.copyOf(originMetamodels);
        this.sourceModelsViewType = new IdentityMappingViewType(String.format("%s_InternalIdentity", name));
    }

    protected abstract Root createStructure();

    protected Root getStructure() {
        if (structure == null) {
            structure = createStructure();
        }
        return structure;
    }

    @Override
    public AllSelector createSelector(ChangeableViewSource source) {
        return new AllSelector(this, source);
    }

    @Override
    public ModifiableView createView(AllSelector selector) {
        return new OperationBasedView(selector);
    }

    @Override
    public void updateView(ModifiableView view) {
        throw new UnsupportedOperationException("Used view class does not call this");
    }

    @Override
    public void commitViewChanges(ModifiableView view, VitruviusChange<HierarchicalId> change) {
        throw new UnsupportedOperationException("Used view class does not call this");
    }

    /**
     * @param closingChain Because a wrapping view (e.g., ChangeDerivingView) might close our source model view when we close it, the closing has to be deferred until the source view is closed.
     */
    private record CommittableOperationBasedView(OperationBasedView view, CommittableView sourceView,
                                                 CommittableOperationBasedView closingChain) implements CommittableView {
        private CommittableOperationBasedView(OperationBasedView view, CommittableView sourceView) {
            this(view, sourceView, null);
        }

        @Override
        public void update() {
            view.update();
        }

        @Override
        public void commitChanges() {
            view.commit();
            sourceView.commitChanges();
        }

        @Override
        public Collection<EObject> getRootObjects() {
            return view.getRootObjects();
        }

        @Override
        public boolean isModified() {
            return view.isModified();
        }

        @Override
        public boolean isOutdated() {
            return sourceView.isOutdated();
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void registerRoot(EObject eObject, URI uri) {
            view.registerRoot(eObject, uri);
        }

        @Override
        public void moveRoot(EObject eObject, URI uri) {
            view.moveRoot(eObject, uri);
        }

        @Override
        public ViewSelection getSelection() {
            return view.getSelection();
        }

        @Override
        public ViewType<? extends ViewSelector> getViewType() {
            return view.getViewType();
        }

        @Override
        public CommittableView withChangeRecordingTrait() {
            return null;
        }

        @Override
        public CommittableView withChangeDerivingTrait(StateBasedChangeResolutionStrategy stateBasedChangeResolutionStrategy) {
            return null;
        }

        @Override
        public void close() throws Exception {
            sourceView.close();
            if (closingChain != null) {
                closingChain.close();
            } else {
                view.close();
            }
        }
    }

    private class OperationBasedView implements ModifiableView {
        private final AllSelector selector;

        private final ViewResourceAccessImpl viewResourceAccess;
        private final ViewWrappingOriginResourceAccessImpl originResourceAccess;
        private final InternalViewImpl internalView;

        private boolean viewChanged = false;
        private boolean closed = false;

        public OperationBasedView(AllSelector selector) {
            this.selector = selector;

            originResourceAccess = new ViewWrappingOriginResourceAccessImpl(createSourceModelsView());
            viewResourceAccess = new ViewResourceAccessImpl(getName());
            internalView = new InternalViewImpl(getStructure(), viewResourceAccess, originResourceAccess);

            update();
        }

        private View createSourceModelsView() {
            var selector = sourceModelsViewType.createSelector(getViewSource());
            selector.getSelectableElements().stream()
                    .filter(eObject -> originMetamodels.contains(eObject.eClass().getEPackage()))
                    .forEach(eObject -> selector.setSelected(eObject, true));
            return selector.createView();
        }

        @Override
        public void update() {
            removeChangeListeners(viewResourceAccess.getResourceSet());

            originResourceAccess.update();

            internalView.update();
            viewChanged = false;

            addChangeListeners(viewResourceAccess.getResourceSet());
        }

        public void commit() {
            internalView.commit();
            viewChanged = false;
        }

        @Override
        public Collection<EObject> getRootObjects() {
            return viewResourceAccess.getRoots();
        }

        @Override
        public boolean isModified() {
            return viewChanged;
        }

        @Override
        public boolean isOutdated() {
            return originResourceAccess.isOutdated();
        }

        @Override
        public void close() throws Exception {
            if (!closed) {
                closed = true;

                internalView.close();

                originResourceAccess.close();

                removeChangeListeners(viewResourceAccess.getResourceSet());

                viewResourceAccess.close();
            }
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void registerRoot(EObject eObject, URI uri) {
            viewResourceAccess.registerRoot(eObject, uri);
        }

        @Override
        public void moveRoot(EObject eObject, URI uri) {
            viewResourceAccess.moveRoot(eObject, uri);
        }

        @Override
        public ViewSelection getSelection() {
            return selector;
        }

        @Override
        public ViewType<? extends ViewSelector> getViewType() {
            return OperationBasedViewType.this;
        }

        @Override
        public CommittableView withChangeRecordingTrait() {
            return new CommittableOperationBasedView(this, originResourceAccess.getView().withChangeRecordingTrait());
        }

        @Override
        public CommittableView withChangeDerivingTrait(StateBasedChangeResolutionStrategy stateBasedChangeResolutionStrategy) {
            return new CommittableOperationBasedView(this, originResourceAccess.getView().withChangeDerivingTrait(stateBasedChangeResolutionStrategy));
        }

        @Override
        public void modifyContents(Consumer<ResourceSet> consumer) {
            consumer.accept(viewResourceAccess.getResourceSet());
        }

        @Override
        public ChangeableViewSource getViewSource() {
            return selector.getViewSource();
        }

        private void addChangeListeners(Notifier notifier) {
            notifier
                    .eAdapters()
                    .add(
                            new AdapterImpl() {
                                @Override
                                public void notifyChanged(Notification notification) {
                                    viewChanged = true;
                                }
                            });
            switch (notifier) {
                case ResourceSet resourceSet -> resourceSet.getResources().forEach(this::addChangeListeners);
                case Resource resource -> resource.getContents().forEach(this::addChangeListeners);
                case EObject eObject -> eObject.eContents().forEach(this::addChangeListeners);
                default -> {
                }
            }
        }

        private void removeChangeListeners(ResourceSet resourceSet) {
            resourceSet.getAllContents().forEachRemaining(eObject -> eObject.eAdapters().clear());
        }
    }
}
