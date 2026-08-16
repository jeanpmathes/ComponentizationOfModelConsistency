package tools.vitruv.compmodelcons.change;

import java.util.List;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.interaction.UserInteractor;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.change.propagation.ModelRepositorySnapshot;

public abstract class AbstractCorrespondenceTranslatingChangeCorrespondenceSpecificationWrapper
    implements CorrespondenceTranslatingChangeCorrespondenceSpecificationWrapper {
  private final ChangePropagationSpecification specification;

  public AbstractCorrespondenceTranslatingChangeCorrespondenceSpecificationWrapper(
      ChangePropagationSpecification specification) {
    if (specification instanceof ViewBasedChangePropagationSpecificationAdapter) {
      throw new IllegalArgumentException(
          "The specification must not be a ViewBasedChangePropagationSpecificationAdapter");
    }

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
  public void setUserInteractor(UserInteractor userInteractor) {
    specification.setUserInteractor(userInteractor);
  }

  @Override
  public void propagateChanges(List<EChange<EObject>> viewChanges,
                               EditableCorrespondenceModelView<Correspondence> correspondenceModel,
                               ViewChangePropagationContext context,
                               ModelRepositorySnapshot previousState) {
    specification.propagateChanges(viewChanges,
                                   wrapCorrespondenceModel(correspondenceModel, context),
                                   context.getResourceAccess(), previousState);
  }

  protected abstract EditableCorrespondenceModelView<Correspondence> wrapCorrespondenceModel(
      EditableCorrespondenceModelView<Correspondence> correspondenceModel,
      ViewChangePropagationContext context);
}
