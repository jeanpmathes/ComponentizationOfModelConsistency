package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.change.propagation.ModelSnapshot;

import java.util.List;

public abstract class AbstractChangePropagationSpecificationWrappingStrategy implements ChangePropagationSpecificationWrappingStrategy {
    private final ChangePropagationSpecification specification;

    public AbstractChangePropagationSpecificationWrappingStrategy(ChangePropagationSpecification specification) {
        this.specification = specification;
    }

    @Override
    public MetamodelDescriptor getSourceMetamodelDescriptor() {
        return specification.getSourceMetamodelDescriptor();
    }

    @Override
    public MetamodelDescriptor getTargetMetamodelDescriptor() {
        return specification.getTargetMetamodelDescriptor();
    }

    @Override
    public boolean doesHandleChange(EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
        return specification.doesHandleChange(eChange, correspondenceModel);
    }

    @Override
    public void propagateChanges(List<EChange<EObject>> viewChanges, EditableCorrespondenceModelView<Correspondence> correspondenceModel, ViewChangePropagationContext context, ModelSnapshot previousState) {
        specification.propagateChanges(viewChanges, wrapCorrespondenceModel(correspondenceModel, context), context.getResourceAccess(), previousState);
    }

    protected abstract EditableCorrespondenceModelView<Correspondence> wrapCorrespondenceModel(EditableCorrespondenceModelView<Correspondence> correspondenceModel, ViewChangePropagationContext context);
}
