package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.change.propagation.impl.AbstractChangePropagationSpecification;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.framework.vsum.VirtualModel;

public class ViewChangePropagationSpecificationAdapter extends AbstractChangePropagationSpecification implements ChangePropagationSpecification {
    private final ViewChangePropagationSpecification sourceViewType;
    private final ChangePropagationSpecificationWrapper specification;
    private final ViewChangePropagationSpecification targetViewType;

    public ViewChangePropagationSpecificationAdapter(ViewChangePropagationSpecification sourceViewType, int sourceViewTypeMetamodelIndex, ChangePropagationSpecificationWrapper specification, ViewChangePropagationSpecification targetViewType, int targetViewTypeMetamodelIndex) {
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
    public boolean doesHandleChange(EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView) {
        return true;
    }

    @Override
    public void propagateChange(EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView, ResourceAccess resourceAccess) {
        // todo: before doing much, let specification directly work with this, intercept how it calls correspondences and resource access

        VirtualModel vsum = null; // todo: get the virtual model

        var sourceView = sourceViewType.getView(vsum); // todo: better handling of null view type
        var targetView = targetViewType.getView(vsum).withChangeDerivingTrait(); // todo: better handling of null view type

        var context = new ViewChangePropagationContext(sourceView, sourceViewType, targetView, targetViewType);

        specification.propagateChange(eChange, editableCorrespondenceModelView, resourceAccess, context);

        targetView.commitChanges();
    }
}
