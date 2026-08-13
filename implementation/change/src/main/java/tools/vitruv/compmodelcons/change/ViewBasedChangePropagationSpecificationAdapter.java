package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.interaction.UserInteractor;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.change.propagation.ModelSnapshot;
import tools.vitruv.change.propagation.impl.AbstractChangePropagationSpecification;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.impl.CorrespondenceResolvingContextImpl;
import tools.vitruv.dsls.reactions.runtime.helper.PersistenceHelper;

import java.util.List;
import java.util.function.Function;

/**
 * This adapter allows using a view-based change propagation specification as a change propagation specification.
 * It combines a {@link ChangePropagationSpecification} with a source and target {@link ChangePropagatingViewTypeSpecification}.
 */
public class ViewBasedChangePropagationSpecificationAdapter extends AbstractChangePropagationSpecification implements ChangePropagationSpecification {
    private final ChangePropagatingViewTypeSpecification sourceViewType;
    private final ChangePropagationSpecificationWrappingStrategy specification;
    private final ChangePropagatingViewTypeSpecification targetViewType;

    private final ChangeDeterminationMode changeDeterminationMode;

    public ViewBasedChangePropagationSpecificationAdapter(ChangePropagatingViewTypeSpecification sourceViewType, MetamodelDescriptor sourceMetamodel, ChangePropagationSpecificationWrappingStrategy specification, ChangePropagatingViewTypeSpecification targetViewType, MetamodelDescriptor targetMetamodel, ChangeDeterminationMode changeDeterminationMode) {
        super(sourceMetamodel, targetMetamodel);

        if (!sourceViewType.getViewTypeMetamodelDescriptor().equals(specification.getSourceMetamodelDescriptor())) {
            throw new IllegalArgumentException("The view type of the source does not match the source metamodel of the change propagation specification");
        }

        if (!specification.getTargetMetamodelDescriptor().equals(targetViewType.getViewTypeMetamodelDescriptor())) {
            throw new IllegalArgumentException("The target metamodel of the change propagation specification does not match the original metamodel of the target");
        }

        this.sourceViewType = sourceViewType;
        this.specification = specification;
        this.targetViewType = targetViewType;
        this.changeDeterminationMode = changeDeterminationMode;
    }

    @Override
    public boolean doesHandleChange(EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
        return true;
    }

    @Override
    public void propagateChanges(List<EChange<EObject>> originChanges, EditableCorrespondenceModelView<Correspondence> correspondenceModel, ResourceAccess changedOrigin, ModelSnapshot previousState) {
        Function<String, URI> uriFactory = changedOrigin.getModelResources().stream()
                                                        .filter(resource -> resource.getURI().isFile()).findAny()
                                                        .map(resource -> resource.getContents().getFirst())
                                                        .map(eObject -> (Function<String, URI>) ((string) -> PersistenceHelper.getURIFromSourceProjectFolder(eObject, string)))
                                                        .orElseThrow();

        CorrespondenceResolvingContext correspondenceContext = new CorrespondenceResolvingContextImpl(changedOrigin);

        try (CorrespondenceModelAccess changedCorrespondenceModel = new CorrespondenceModelAccess(correspondenceModel);
             ModelSnapshot unchangedOrigin = previousState.copy();
             CorrespondenceModelAccess unchangedCorrespondenceModel = changedCorrespondenceModel.copy(unchangedOrigin);
             ChangePropagationView sourceView = sourceViewType.createView(unchangedOrigin, unchangedCorrespondenceModel, uriFactory, this, correspondenceContext);
             ChangePropagationView targetView = targetViewType.createView(changedOrigin, changedCorrespondenceModel, uriFactory, this, correspondenceContext)
        ) {
            List<EChange<EObject>> viewChanges = sourceView.fitAndDetermineChanges(changedOrigin, changedCorrespondenceModel, originChanges, changeDeterminationMode);

            var context = new ViewChangePropagationContext(sourceView, sourceViewType, targetView, targetViewType);

            originChanges.forEach(change -> notifyChangePropagationStarted(this, change));

            // For full correctness, the previous state would need to include the unchanged state of the two views as well.
            // However, no one is using that anyway, so why bother?
            specification.propagateChanges(viewChanges, correspondenceModel, context, null);
            targetView.commit();

            originChanges.forEach(change -> notifyChangePropagationStopped(this, change));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void propagateChange(EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView, ResourceAccess resourceAccess) {
        throw new UnsupportedOperationException("This method should not be called, use propagateChanges instead");
    }

    @Override
    public void setUserInteractor(UserInteractor userInteractor) {
        super.setUserInteractor(userInteractor);
        specification.setUserInteractor(userInteractor);
    }
}
