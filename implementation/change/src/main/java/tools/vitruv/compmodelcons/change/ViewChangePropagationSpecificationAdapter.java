package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.change.propagation.ModelSnapshot;
import tools.vitruv.change.propagation.impl.AbstractChangePropagationSpecification;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.impl.CorrespondenceResolvingContextImpl;
import tools.vitruv.dsls.reactions.runtime.helper.PersistenceHelper;

import java.util.List;
import java.util.function.Function;

public class ViewChangePropagationSpecificationAdapter extends AbstractChangePropagationSpecification implements ChangePropagationSpecification {
    private final ChangePropagationViewTypeSpecification sourceViewType;
    private final int sourceViewTypeMetamodelIndex;
    private final ChangePropagationSpecificationWrappingStrategy specification;
    private final ChangePropagationViewTypeSpecification targetViewType;
    private final int targetViewTypeMetamodelIndex;
    private final ChangeDeterminationMode changeDeterminationMode;

    public ViewChangePropagationSpecificationAdapter(ChangePropagationViewTypeSpecification sourceViewType, int sourceViewTypeMetamodelIndex, ChangePropagationSpecificationWrappingStrategy specification, ChangePropagationViewTypeSpecification targetViewType, int targetViewTypeMetamodelIndex, ChangeDeterminationMode changeDeterminationMode) {
        super(sourceViewType.getOriginMetamodelDescriptors().get(sourceViewTypeMetamodelIndex), targetViewType.getOriginMetamodelDescriptors().get(targetViewTypeMetamodelIndex));

        if (!sourceViewType.getViewTypeMetamodelDescriptor().equals(specification.getSourceMetamodelDescriptor())) {
            throw new IllegalArgumentException("The view type of the source does not match the source metamodel of the change propagation specification");
        }

        if (!specification.getTargetMetamodelDescriptor().equals(targetViewType.getViewTypeMetamodelDescriptor())) {
            throw new IllegalArgumentException("The target metamodel of the change propagation specification does not match the origina metamodel of the target");
        }

        this.sourceViewType = sourceViewType;
        this.sourceViewTypeMetamodelIndex = sourceViewTypeMetamodelIndex;
        this.specification = specification;
        this.targetViewType = targetViewType;
        this.targetViewTypeMetamodelIndex = targetViewTypeMetamodelIndex;
        this.changeDeterminationMode = changeDeterminationMode;
    }

    @Override
    public boolean doesHandleChange(EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
        return specification.doesHandleChange(eChange, correspondenceModel);
    }

    @Override
    public void propagateChanges(List<EChange<EObject>> originChanges, EditableCorrespondenceModelView<Correspondence> correspondenceModel, ResourceAccess changedOrigin, ModelSnapshot previousState) {
        Function<String, URI> uriFactory = changedOrigin.getModelResources().stream()
                .filter(resource -> resource.getURI().isFile())
                .findAny()
                .map(resource -> resource.getContents().getFirst())
                .map(eObject -> (Function<String, URI>) ((string) -> PersistenceHelper.getURIFromSourceProjectFolder(eObject, string)))
                .orElseThrow();

        CorrespondenceResolvingContext correspondenceContext = new CorrespondenceResolvingContextImpl(changedOrigin);

        try (
                ModelSnapshot unchangedOrigin = previousState.copy();
                ChangePropagationView sourceView = sourceViewType.createView(sourceViewTypeMetamodelIndex, unchangedOrigin, uriFactory, correspondenceContext);
                ChangePropagationView targetView = targetViewType.createView(targetViewTypeMetamodelIndex, changedOrigin, uriFactory, correspondenceContext)
        ) {
            List<EChange<EObject>> viewChanges = sourceView.fitAndDetermineChanges(changedOrigin, originChanges, changeDeterminationMode);

            var context = new ViewChangePropagationContext(sourceView, sourceViewType, targetView, targetViewType);
            // For full correctness, the previous state would need to include the unchanged state of the two views as well.
            // However, no one is using that anyway, so why bother?
            specification.propagateChanges(viewChanges, correspondenceModel, context, null);

            targetView.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void propagateChange(EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView, ResourceAccess resourceAccess) {
        throw new UnsupportedOperationException("This method should not be called, use propagateChanges instead");
    }
}
