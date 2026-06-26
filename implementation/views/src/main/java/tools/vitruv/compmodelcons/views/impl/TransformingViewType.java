package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.atomic.uuid.Uuid;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.framework.views.ChangeableViewSource;
import tools.vitruv.framework.views.impl.AbstractViewType;
import tools.vitruv.framework.views.impl.BasicView;
import tools.vitruv.framework.views.impl.ModifiableView;

import java.util.Collection;
import java.util.List;

public abstract class TransformingViewType extends AbstractViewType<DefaultSelector, HierarchicalId> {

    public TransformingViewType(String name, EPackage metaModel) {
        super(name, metaModel);
    }

    protected abstract void generateView(Collection<Resource> sources, List<Resource> target);

    protected abstract VitruviusChange<Uuid> transformChange(Collection<Resource> sources, VitruviusChange<HierarchicalId> change);

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

            generateView(viewSource.getViewSourceModels(), viewResources);
        });
    }

    @Override
    public void commitViewChanges(ModifiableView view, VitruviusChange<HierarchicalId> change) {
        var viewSource = view.getViewSource();

        var transformedChange = transformChange(viewSource.getViewSourceModels(), change);

        viewSource.propagateChange(transformedChange);
    }

}
