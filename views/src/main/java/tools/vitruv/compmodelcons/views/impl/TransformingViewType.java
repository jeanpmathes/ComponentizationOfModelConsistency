package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.atomic.uuid.Uuid;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.CorrespondenceModelView;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.framework.views.ChangeableViewSource;
import tools.vitruv.framework.views.impl.AbstractViewType;
import tools.vitruv.framework.views.impl.BasicView;
import tools.vitruv.framework.views.impl.ModifiableView;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class TransformingViewType extends AbstractViewType<DefaultSelector, HierarchicalId> {

    public TransformingViewType(String name, EPackage metaModel, URI viewUri) {
        super(name, metaModel);
    }

    protected abstract void generateView(Collection<Resource> sources, Optional<CorrespondenceModelView<? extends Correspondence>> correspondenceModel, List<Resource> target);
    protected abstract VitruviusChange<Uuid> transformChange(Collection<Resource> sources, Optional<EditableCorrespondenceModelView<? extends Correspondence>> correspondenceModel, VitruviusChange<HierarchicalId> change);

    @Override
    public DefaultSelector createSelector(ChangeableViewSource source) {
        return new DefaultSelector(this, source);
    }

    @Override
    public ModifiableView createView(DefaultSelector selector) {
        return new BasicView(this, selector.getViewSource(), selector.getSelection());
    }

    @Override
    public void updateView(ModifiableView view) {
        view.modifyContents(viewResourceSet -> {
            var viewResources = viewResourceSet.getResources();
            viewResources.forEach(Resource::unload);
            viewResources.clear();

            var viewSource = view.getViewSource();

            Optional<CorrespondenceModelView<? extends Correspondence>> correspondenceModel = Optional.empty();

            // todo: determine whether this is necessary
            // if (viewSource instanceof InternalVirtualModel ivm) {
            //    correspondenceModel = Optional.of(ivm.getCorrespondenceModel());
            // }
            
            generateView(viewSource.getViewSourceModels(), correspondenceModel, viewResources);
        });
    }

    @Override
    public void commitViewChanges(ModifiableView view, VitruviusChange<HierarchicalId> change) {
        var viewSource = view.getViewSource();

        Optional<EditableCorrespondenceModelView<? extends Correspondence>> correspondenceModel = Optional.empty();

        // todo: determine whether this is necessary
        // if (viewSource instanceof InternalVirtualModel ivm) {
        //    correspondenceModel = Optional.of(ivm.getCorrespondenceModel());
        // }

        var transformedChange = transformChange(viewSource.getViewSourceModels(), correspondenceModel, change);
        viewSource.propagateChange(transformedChange);
    }

}
