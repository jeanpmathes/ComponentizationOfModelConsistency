package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.compmodelcons.change.ViewChangePropagationSpecification;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;

import java.util.Collection;

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

    protected abstract Collection<Class<?>> getRootTypes();
}
