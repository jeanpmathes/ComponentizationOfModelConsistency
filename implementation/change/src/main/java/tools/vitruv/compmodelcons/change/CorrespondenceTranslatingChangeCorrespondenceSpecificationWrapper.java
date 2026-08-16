package tools.vitruv.compmodelcons.change;

import java.util.List;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.interaction.UserInteractor;
import tools.vitruv.change.propagation.ModelRepositorySnapshot;

public interface CorrespondenceTranslatingChangeCorrespondenceSpecificationWrapper {
  MetamodelDescriptor getSourceMetamodelDescriptor();

  MetamodelDescriptor getTargetMetamodelDescriptor();

  void setUserInteractor(UserInteractor userInteractor);

  void propagateChanges(List<EChange<EObject>> viewChanges,
                        EditableCorrespondenceModelView<Correspondence> correspondenceModel,
                        ViewChangePropagationContext context,
                        ModelRepositorySnapshot previousState);
}
