package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.change.utils.ResourceAccess;

public abstract class AbstractChangePropagationSpecificationWrapper implements ChangePropagationSpecificationWrapper {
    private final ChangePropagationSpecification specification;

    public AbstractChangePropagationSpecificationWrapper(ChangePropagationSpecification specification) {
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
    public boolean doesHandleChange(EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView) {
        return specification.doesHandleChange(eChange, editableCorrespondenceModelView);
    }

    @Override
    public void propagateChange(EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView, ResourceAccess resourceAccess, ViewChangePropagationContext context) {
        EChange<EObject> liftedEChange = liftEChange(eChange);
        EditableCorrespondenceModelView<Correspondence> liftedEditableCorrespondenceModelView = getLiftedCorrespondenceModel(editableCorrespondenceModelView);
        ResourceAccess liftedResourceAccess = getLiftedResourceAccess(resourceAccess);

        specification.propagateChange(liftedEChange, liftedEditableCorrespondenceModelView, liftedResourceAccess);

        // Lowering of the correspondences and resource changes happens during change propagation using the classes.
    }

    private EChange<EObject> liftEChange(EChange<EObject> eChange) {
        return eChange;
    }

    protected abstract LiftedEditableCorrespondenceModelView getLiftedCorrespondenceModel(EditableCorrespondenceModelView<Correspondence> correspondenceModel);

    private LiftedResourceAccess getLiftedResourceAccess(ResourceAccess resourceAccess) {
        return new LiftedResourceAccess();
    }

    private class LiftedResourceAccess implements ResourceAccess {
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

    protected abstract class LiftedEditableCorrespondenceModelView implements EditableCorrespondenceModelView<Correspondence> {

    }
}
