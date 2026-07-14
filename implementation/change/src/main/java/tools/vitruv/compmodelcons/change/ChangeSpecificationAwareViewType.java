package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.views.impl.OperationBasedViewType;

import java.util.List;

public abstract class ChangeSpecificationAwareViewType extends OperationBasedViewType implements ChangePropagationViewTypeSpecification {
    public ChangeSpecificationAwareViewType(String name, List<EPackage> originMetamodels, EPackage viewTypeMetamodel) {
        super(name, originMetamodels, viewTypeMetamodel);
    }

    @Override
    public ChangePropagationView createView(ResourceAccess resourceAccess) {
        return new ChangePropagationViewImpl();
    }

    private class ChangePropagationViewImpl implements ChangePropagationView {

        // todo: when implementing this, keep duplication with view ops in OperationBasedView low, maybe add shared base class or at least utilities

        @Override
        public ResourceAccess getViewResourceAccess() {
            return null;
        }

        @Override
        public List<EChange<EObject>> doGetChange(EChange<EObject> originChange) {
            return List.of();
        }

        @Override
        public void beginChangeRecording() {

        }

        @Override
        public void commitRecordedChanges() {

        }
    }
}
