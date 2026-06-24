package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.atomic.uuid.Uuid;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.CorrespondenceModelView;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.compmodelcons.change.ViewChangePropagationSpecification;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class ChangeSpecificationAwareViewType extends TransformingViewType implements ViewChangePropagationSpecification {
    public ChangeSpecificationAwareViewType(String name, EPackage metaModel) {
        super(name, metaModel);
    }

    @Override
    public View getView(VirtualModel vsum) { // todo: use the generated NeoJoin view type instead of identity mapping
        var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
        selector.getSelectableElements().stream()
                .filter(element -> getRootTypes().stream().anyMatch(it -> it.isInstance(element)))
                .forEach(it -> selector.setSelected(it, true));
        return selector.createView();
    }

    @Override
    protected void generateView(Collection<Resource> sources, Optional<CorrespondenceModelView<? extends Correspondence>> correspondenceModel, List<Resource> target) {

    }

    @Override
    protected VitruviusChange<Uuid> transformChange(Collection<Resource> sources, Optional<EditableCorrespondenceModelView<? extends Correspondence>> correspondenceModel, VitruviusChange<HierarchicalId> change) {
        return null;
    }

    protected abstract Collection<Class<?>> getRootTypes();
}
