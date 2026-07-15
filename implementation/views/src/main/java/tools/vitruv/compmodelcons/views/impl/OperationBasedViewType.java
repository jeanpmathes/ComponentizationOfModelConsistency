package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.EChangeUtil;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.change.atomic.eobject.EObjectSubtractedEChange;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.recording.ChangeRecorder;
import tools.vitruv.compmodelcons.views.EditableViewCorrespondences;
import tools.vitruv.compmodelcons.views.operations.Root;
import tools.vitruv.framework.views.*;
import tools.vitruv.framework.views.changederivation.StateBasedChangeResolutionStrategy;
import tools.vitruv.framework.views.impl.AbstractViewType;
import tools.vitruv.framework.views.impl.IdentityMappingViewType;
import tools.vitruv.framework.views.impl.ModifiableView;

import java.util.ArrayList;
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

    private Root getStructure() {
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

        private final EditableViewCorrespondences correspondences = new EditableViewCorrespondencesImpl();

        private final ViewResourceAccessImpl viewResourceAccess;
        private final ViewWrappingOriginResourceAccessImpl originResourceAccess;

        private Root.ViewBinding viewBinding;

        private ChangeRecorder changeRecorder;

        private boolean viewChanged = false;
        private boolean closed = false;

        public OperationBasedView(AllSelector selector) {
            this.selector = selector;

            originResourceAccess = new ViewWrappingOriginResourceAccessImpl(createSourceModelsView());
            viewResourceAccess = new ViewResourceAccessImpl(getName());

            setupChangeRecorderAndBeginRecording();

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
            originResourceAccess.update();

            endRecordingAndClose();

            viewResourceAccess.reset();
            viewBinding = getStructure().doGet(new GetContextImpl(originResourceAccess, viewResourceAccess, correspondences));
            viewChanged = false;

            addChangeListeners(viewResourceAccess.getResourceSet());

            setupChangeRecorderAndBeginRecording();
        }

        public void commit() {
            VitruviusChange<EObject> change = changeRecorder.endRecording();

            var context = new PutContextImpl(originResourceAccess, viewResourceAccess, correspondences);
            reorderChanges(change.getEChanges()).forEach(eChange -> viewBinding = getStructure().doPut(eChange, viewBinding, context));

            context.validateAttachmentState();

            viewChanged = false;
            changeRecorder.beginRecording();
        }

        private List<EChange<EObject>> reorderChanges(List<EChange<EObject>> changes) {
            // While the documentation of ChangeRecorder.endRecording() states that the deletions are inserted
            // right after the change causing the removal, that is not the case.
            // See ChangeRecorder.postprocessRemovals() and its documentation.
            // However, I would really like it to be the case.

            List<EChange<EObject>> reorderedChanges = new ArrayList<>(changes);

            int numberOfDeletionsToReorder = 0;
            for (int index = reorderedChanges.size() - 1; index >= 0; index--) {
                if (reorderedChanges.get(index) instanceof DeleteEObject<EObject>) {
                    numberOfDeletionsToReorder += 1;
                } else {
                    break;
                }
            }

            while (numberOfDeletionsToReorder > 0) {
                DeleteEObject<EObject> deletion = (DeleteEObject<EObject>) reorderedChanges.getLast();
                reorderedChanges.removeLast();

                List<EChange<EObject>> associatedDeletions = new ArrayList<>();
                for (int index = reorderedChanges.size() - 1; index >= 0; index--) {
                    EChange<EObject> eChange = reorderedChanges.get(index);
                    if (eChange instanceof DeleteEObject<EObject> subtractedEChange
                            && EChangeUtil.isContainmentRemoval(subtractedEChange)
                            && subtractedEChange.getAffectedElement() == deletion.getAffectedElement()) {
                        associatedDeletions.addFirst(eChange);
                        reorderedChanges.remove(index);
                    } else {
                        break;
                    }
                }

                int indexOfCause = -1;

                for (int index = reorderedChanges.size() - 1; index >= 0; index--) {
                    EChange<EObject> eChange = reorderedChanges.get(index);
                    if (eChange instanceof EObjectSubtractedEChange<EObject> subtractedEChange && EChangeUtil.isContainmentRemoval(subtractedEChange) && subtractedEChange.getOldValue() == deletion.getAffectedElement()) {
                        indexOfCause = index;
                        break;
                    }
                }

                if (indexOfCause == -1) {
                    throw new IllegalStateException("Could not find the cause of the deletion");
                }

                int indexRightAfterCause = indexOfCause + 1;
                reorderedChanges.add(indexRightAfterCause, deletion);
                reorderedChanges.addAll(indexRightAfterCause, associatedDeletions);

                numberOfDeletionsToReorder -= associatedDeletions.size() + 1;
            }

            return reorderedChanges;
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

        private void setupChangeRecorderAndBeginRecording() {
            changeRecorder = new ChangeRecorder(viewResourceAccess.getResourceSet());
            changeRecorder.addToRecording(viewResourceAccess.getResourceSet());
            changeRecorder.beginRecording();
        }

        private void endRecordingAndClose() {
            if (changeRecorder.isRecording()) {
                changeRecorder.endRecording();
            }
            changeRecorder.close();
            changeRecorder = null;
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
