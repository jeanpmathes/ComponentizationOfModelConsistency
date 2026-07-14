package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.change.propagation.impl.AbstractChangePropagationSpecification;
import tools.vitruv.change.utils.ResourceAccess;

import java.util.List;

public class ViewChangePropagationSpecificationAdapter extends AbstractChangePropagationSpecification implements ChangePropagationSpecification {
    private final ChangePropagationViewTypeSpecification sourceViewType;
    private final ChangePropagationSpecificationWrappingStrategy specification;
    private final ChangePropagationViewTypeSpecification targetViewType;

    public ViewChangePropagationSpecificationAdapter(ChangePropagationViewTypeSpecification sourceViewType, int sourceViewTypeMetamodelIndex, ChangePropagationSpecificationWrappingStrategy specification, ChangePropagationViewTypeSpecification targetViewType, int targetViewTypeMetamodelIndex) {
        super(sourceViewType.getOriginMetamodelDescriptors().get(sourceViewTypeMetamodelIndex), targetViewType.getOriginMetamodelDescriptors().get(targetViewTypeMetamodelIndex));

        if (!sourceViewType.getViewTypeMetamodelDescriptor().equals(specification.getSourceMetamodelDescriptor())) {
            throw new IllegalArgumentException("The view type of the source does not match the source metamodel of the change propagation specification");
        }

        if (!specification.getTargetMetamodelDescriptor().equals(targetViewType.getViewTypeMetamodelDescriptor())) {
            throw new IllegalArgumentException("The target metamodel of the change propagation specification does not match the origina metamodel of the target");
        }

        this.sourceViewType = sourceViewType;
        this.specification = specification;
        this.targetViewType = targetViewType;
    }

    @Override
    public boolean doesHandleChange(EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
        return specification.doesHandleChange(eChange, correspondenceModel);
    }

    @Override
    public void propagateChanges(List<EChange<EObject>> originChanges, EditableCorrespondenceModelView<Correspondence> correspondenceModel, ResourceAccess resourceAccess) {
        ChangePropagationView sourceView = sourceViewType.createView(resourceAccess);
        ChangePropagationView targetView = targetViewType.createView(resourceAccess);
        var context = new ViewChangePropagationContext(sourceView, sourceViewType, targetView, targetViewType);

        List<EChange<EObject>> viewChanges = sourceView.doGetChange(originChanges);

        targetView.beginChangeRecording();
        specification.propagateChanges(viewChanges, correspondenceModel, context);
        targetView.commitRecordedChanges();
    }

    @Override
    public void propagateChange(EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView, ResourceAccess resourceAccess) {
        throw new UnsupportedOperationException("This method should not be called, use propagateChanges instead");
    }
}
