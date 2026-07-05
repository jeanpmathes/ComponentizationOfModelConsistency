package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.compmodelcons.views.impl.OperationBasedViewType;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.vsum.VirtualModel;

public abstract class ChangeSpecificationAwareViewType extends OperationBasedViewType implements ViewChangePropagationSpecification {
    public ChangeSpecificationAwareViewType(String name, EPackage metaModel) {
        super(name);
    }

    @Override
    public View getView(VirtualModel vsum) {
        return vsum.createSelector(this).createView();
    }
}
