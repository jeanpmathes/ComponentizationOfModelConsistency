package tools.vitruv.compmodelcons.views.impl;

import edu.kit.ipd.sdq.commons.util.org.eclipse.emf.ecore.resource.ResourceSetUtil;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.recording.ChangeRecorder;
import tools.vitruv.compmodelcons.views.Context;
import tools.vitruv.compmodelcons.views.EditableViewCorrespondences;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.operations.Operation;
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
    private final static String ViewTypeMustBeUsedWithViewsCreatedByItMessage = "Viewtype must only be used in combination with views created by it";

    private final List<EPackage> originMetamodels;
    private final IdentityMappingViewType sourceModelsViewType;

    private Operation structure;

    public OperationBasedViewType(String name, List<EPackage> originMetamodels, EPackage viewTypeMetamodel) {
        super(name, viewTypeMetamodel);

        this.originMetamodels = List.copyOf(originMetamodels);
        this.sourceModelsViewType = new IdentityMappingViewType(String.format("%s_InternalIdentity", name));
    }

    protected abstract Operation createStructure();

    private Operation getStructure() {
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
        private final ResourceSet viewResourceSet;

        private final View sourceView;

        private final EditableViewCorrespondences correspondences = new EditableViewCorrespondencesImpl();

        private ChangeRecorder changeRecorder;

        private boolean viewChanged = false;

        private Resource viewModel;
        private ObjectBinding viewRoot = ObjectBinding.empty();

        private boolean closed = false;

        public OperationBasedView(AllSelector selector) {
            this.selector = selector;

            sourceView = createSourceModelsView();
            viewResourceSet = ResourceSetUtil.withGlobalFactories(new ResourceSetImpl());

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
            sourceView.update();

            endRecordingAndClose();

            viewResourceSet.getResources().forEach(Resource::unload);
            viewResourceSet.getResources().clear();

            viewModel = viewResourceSet.createResource(URI.createURI(String.format("view:/%s.view", getName())));
            viewRoot = unwrap(getStructure().get(new GetContextImpl()));

            viewChanged = false;
            addChangeListeners(viewResourceSet);

            setupChangeRecorderAndBeginRecording();
        }

        public void commit() {
            VitruviusChange<EObject> change = changeRecorder.endRecording();

            PutContext context = new PutContextImpl();
            change.getEChanges().forEach(eChange -> viewRoot = getStructure().put(eChange, viewRoot, context));

            viewChanged = false;
            changeRecorder.beginRecording();
        }

        @Override
        public Collection<EObject> getRootObjects() {
            return viewResourceSet.getResources().stream()
                    .flatMap(resource -> resource.getContents().stream())
                    .toList();
        }

        @Override
        public boolean isModified() {
            return viewChanged;
        }

        @Override
        public boolean isOutdated() {
            return sourceView.isOutdated();
        }

        @Override
        public void close() throws Exception {
            if (!closed) {
                closed = true;

                sourceView.close();

                viewResourceSet.getResources().forEach(Resource::unload);
                viewResourceSet.getResources().clear();

                removeChangeListeners(viewResourceSet);
            }
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void registerRoot(EObject eObject, URI uri) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void moveRoot(EObject eObject, URI uri) {
            throw new UnsupportedOperationException();
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
            return new CommittableOperationBasedView(this, sourceView.withChangeRecordingTrait());
        }

        @Override
        public CommittableView withChangeDerivingTrait(StateBasedChangeResolutionStrategy stateBasedChangeResolutionStrategy) {
            return new CommittableOperationBasedView(this, sourceView.withChangeDerivingTrait(stateBasedChangeResolutionStrategy));
        }

        @Override
        public void modifyContents(Consumer<ResourceSet> consumer) {
            consumer.accept(viewResourceSet);
        }

        @Override
        public ChangeableViewSource getViewSource() {
            return selector.getViewSource();
        }

        private void setupChangeRecorderAndBeginRecording() {
            changeRecorder = new ChangeRecorder(viewResourceSet);
            changeRecorder.addToRecording(viewResourceSet);
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
            if (notifier instanceof ResourceSet resourceSet) {
                resourceSet.getResources().forEach(this::addChangeListeners);
            } else if (notifier instanceof Resource resource) {
                resource.getContents().forEach(this::addChangeListeners);
            } else if (notifier instanceof EObject eObject) {
                eObject.eContents().forEach(this::addChangeListeners);
            }
        }

        private void removeChangeListeners(ResourceSet resourceSet) {
            resourceSet.getAllContents().forEachRemaining(eObject -> eObject.eAdapters().clear());
        }

        private ObjectBinding unwrap(List<ObjectBinding> bindings) {
            if (bindings.size() != 1) {
                throw new UnsupportedOperationException("Views with no or multiple roots are currently not supported");
            }
            return bindings.get(0);
        }

        private EObject getOriginRoot(EPackage ePackage) {
            return sourceView.getRootObjects().stream()
                    .filter(eObject -> eObject.eClass().getEPackage().equals(ePackage))
                    .findFirst()
                    .orElseThrow();
        }

        private class AbstractContext implements Context {

            @Override
            public List<EObject> getOriginObjects(EClass eClass) {
                List<EObject> result = new ArrayList<>();
                EObject root = getOriginRoot(eClass.getEPackage());
                if (eClass.isSuperTypeOf(root.eClass())) {
                    result.add(root);
                }
                var iterator = root.eAllContents();
                while (iterator.hasNext()) {
                    EObject eObject = iterator.next();
                    if (eClass.isSuperTypeOf(eObject.eClass())) {
                        result.add(eObject);
                    }
                }
                return result;
            }

            @Override
            public EditableViewCorrespondences getCorrespondences() {
                return correspondences;
            }
        }

        private class GetContextImpl extends AbstractContext implements GetContext {
            @Override
            public Resource getViewModel() {
                return viewModel;
            }
        }

        private class PutContextImpl extends AbstractContext implements PutContext {
            @Override
            public void addRootToOriginModel(EPackage originPackage, EObject originObject) {
                // The main reason for this is that the View interface does not offer the methods to do so.
                throw new UnsupportedOperationException("Adding roots to origin models through views not supported");
            }

            @Override
            public void removeRootFromOriginModel(EPackage originPackage, EObject originObject) {
                // The main reason for this is that the View interface does not offer the methods to do so.
                throw new UnsupportedOperationException("Removing roots from origin models through views not supported");
            }
        }
    }
}
