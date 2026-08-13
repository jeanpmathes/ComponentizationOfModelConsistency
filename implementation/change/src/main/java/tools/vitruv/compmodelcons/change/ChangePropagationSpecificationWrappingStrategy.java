package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.interaction.UserInteractor;
import tools.vitruv.change.propagation.ModelSnapshot;

import java.util.List;

public interface ChangePropagationSpecificationWrappingStrategy {
    MetamodelDescriptor getSourceMetamodelDescriptor();

    MetamodelDescriptor getTargetMetamodelDescriptor();

    void setUserInteractor(UserInteractor userInteractor);

    void propagateChanges(List<EChange<EObject>> viewChanges, EditableCorrespondenceModelView<Correspondence> correspondenceModel, ViewChangePropagationContext context, ModelSnapshot previousState);
}
