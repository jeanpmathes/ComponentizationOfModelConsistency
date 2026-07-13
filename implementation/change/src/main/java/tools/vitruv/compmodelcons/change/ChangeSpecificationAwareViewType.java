package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.compmodelcons.views.impl.OperationBasedViewType;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewProvider;

import java.util.List;

public abstract class ChangeSpecificationAwareViewType extends OperationBasedViewType implements ViewChangePropagationSpecification {
    public ChangeSpecificationAwareViewType(String name, List<EPackage> originMetamodels, EPackage viewTypeMetamodel) {
        super(name, originMetamodels, viewTypeMetamodel);
    }

    @Override
    public View getView(ViewProvider viewProvider) {
        return viewProvider.createSelector(this).createView();
    }
}
