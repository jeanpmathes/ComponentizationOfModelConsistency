package tools.vitruv.compmodelcons.change.impl;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.ChangePropagationSpecificationWrapper;
import tools.vitruv.compmodelcons.change.CorrespondenceHandlingStrategy;
import tools.vitruv.compmodelcons.change.ViewChangePropagationContext;

public class ChangePropagationSpecificationWrapperImpl implements ChangePropagationSpecificationWrapper {
    private final ChangePropagationSpecification specification;
    private final CorrespondenceHandlingStrategy correspondenceHandlingStrategy;

    public ChangePropagationSpecificationWrapperImpl(ChangePropagationSpecification specification, CorrespondenceHandlingStrategy correspondenceHandlingStrategy) {
        this.specification = specification;
        this.correspondenceHandlingStrategy = correspondenceHandlingStrategy;
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
    public boolean doesHandleChange(EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView) {
        return specification.doesHandleChange(eChange, editableCorrespondenceModelView);
    }

    @Override
    public void propagateChange(EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView, ResourceAccess resourceAccess, ViewChangePropagationContext context) {
        EChange<EObject> liftedEChange = liftEChange(eChange);
        EditableCorrespondenceModelView<Correspondence> liftedEditableCorrespondenceModelView = correspondenceHandlingStrategy.getLiftedCorrespondenceModel(editableCorrespondenceModelView);
        ResourceAccess liftedResourceAccess = getLiftedResourceAccess(resourceAccess);

        specification.propagateChange(liftedEChange, liftedEditableCorrespondenceModelView, liftedResourceAccess);

        // Lowering of the correspondences and resource changes happens during change propagation.
    }

    private EChange<EObject> liftEChange(EChange<EObject> eChange) {
        return eChange;
    }

    private ViewBasedResourceAccess getLiftedResourceAccess(ResourceAccess resourceAccess) {
        return new ViewBasedResourceAccess();
    }

    private static class ViewBasedResourceAccess implements ResourceAccess {
        @Override
        public URI getMetadataModelURI(String... strings) {
            return null;
        }

        @Override
        public Resource getModelResource(URI uri) {
            return null;
        }

        @Override
        public void persistAsRoot(EObject eObject, URI uri) {

        }
    }
}
