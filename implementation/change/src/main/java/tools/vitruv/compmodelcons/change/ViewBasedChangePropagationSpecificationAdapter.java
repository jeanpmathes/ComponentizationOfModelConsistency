package tools.vitruv.compmodelcons.change;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.function.Function;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.interaction.UserInteractor;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.change.propagation.ModelRepositorySnapshot;
import tools.vitruv.change.propagation.impl.AbstractChangePropagationSpecification;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceResolverFactory;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceTranslationStrategy;
import tools.vitruv.dsls.reactions.runtime.helper.PersistenceHelper;

/**
 * This adapter allows using a view-based change propagation specification as a change
 * propagation specification.
 * It combines a {@link ChangePropagationSpecification} with a source and target
 * {@link ChangePropagatingViewTypeSpecification}.
 */
public class ViewBasedChangePropagationSpecificationAdapter
    extends AbstractChangePropagationSpecification implements ChangePropagationSpecification {
  private final ChangePropagatingViewTypeSpecification sourceViewType;
  private final ChangePropagationSpecification specification;
  private final CorrespondenceTranslationStrategy correspondenceTranslationStrategy;
  private final ChangePropagatingViewTypeSpecification targetViewType;

  ViewBasedChangePropagationSpecificationAdapter(
      ChangePropagatingViewTypeSpecification sourceViewType, MetamodelDescriptor sourceMetamodel,
      ChangePropagationSpecification specification,
      CorrespondenceTranslationStrategy correspondenceTranslationStrategy,
      ChangePropagatingViewTypeSpecification targetViewType, MetamodelDescriptor targetMetamodel) {
    super(sourceMetamodel, targetMetamodel);

    if (specification instanceof ViewBasedChangePropagationSpecificationAdapter) {
      throw new IllegalArgumentException(
          "The specification must not be a ViewBasedChangePropagationSpecificationAdapter");
    }

    if (!sourceViewType
        .getViewTypeMetamodelDescriptor()
        .equals(specification.getSourceMetamodelDescriptor())) {
      throw new IllegalArgumentException(
          "The view type of the source does not match the source metamodel of the change "
              + "propagation specification");
    }

    if (!specification
        .getTargetMetamodelDescriptor()
        .equals(targetViewType.getViewTypeMetamodelDescriptor())) {
      throw new IllegalArgumentException(
          "The target metamodel of the change propagation specification does not match the "
              + "original metamodel of the target");
    }

    this.sourceViewType = sourceViewType;
    this.specification = specification;
    this.correspondenceTranslationStrategy = correspondenceTranslationStrategy;
    this.targetViewType = targetViewType;
  }

  private static Function<String, URI> createUriFactory(ResourceAccess changedOrigin,
                                                        ModelRepositorySnapshot unchangedOrigin) {
    return Streams
        .concat(changedOrigin
                    .getModelResources()
                    .stream(), unchangedOrigin
                    .getModelResources()
                    .stream())
        .filter(resource -> !resource
            .getContents()
            .isEmpty())
        .map(resource -> resource
            .getContents()
            .getFirst())
        .findFirst()
        .map(
            eObject -> (Function<String, URI>) ((string) ->
                PersistenceHelper.getURIFromSourceProjectFolder(eObject, string)))
        .orElseThrow();
  }

  @Override
  public boolean doesHandleChange(
      EChange<EObject> eChange,
      EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
    return true;
  }

  @Override
  public void propagateChanges(
      List<EChange<EObject>> originChanges,
      EditableCorrespondenceModelView<Correspondence> changedCorrespondenceModel,
      ResourceAccess changedOrigin,
      ModelRepositorySnapshot unchangedOrigin) {
    Function<String, URI> uriFactory = createUriFactory(changedOrigin, unchangedOrigin);

    CorrespondenceResolverFactory correspondenceResolverFactory =
        correspondenceTranslationStrategy.createCorrespondenceResolverFactory(changedOrigin);

    try (
        CorrespondenceModelAccess unchangedCorrespondenceModelAccess =
            new CorrespondenceModelAccess(
                unchangedOrigin.getCorrespondenceModel());
        CorrespondenceModelAccess changedCorrespondenceModelAccess = new CorrespondenceModelAccess(
            changedCorrespondenceModel);
        ChangePropagationView sourceView
            = sourceViewType.createView(unchangedOrigin,
                                        unchangedCorrespondenceModelAccess,
                                        uriFactory,
                                        this,
                                        correspondenceResolverFactory);
        ChangePropagationView targetView
            = targetViewType.createView(changedOrigin,
                                        changedCorrespondenceModelAccess,
                                        uriFactory,
                                        this,
                                        correspondenceResolverFactory)
    ) {
      List<EChange<EObject>> viewChanges =
          sourceView.fitAndDetermineChanges(changedOrigin, changedCorrespondenceModelAccess,
                                            originChanges);

      var context =
          new ViewChangePropagationContext(sourceView, sourceViewType, targetView, targetViewType);

      originChanges.forEach(change -> notifyChangePropagationStarted(this, change));

      // For full correctness, the previous state would need to include the unchanged state of
      // the two views as well.
      // However, no one is using that anyway, so I have chosen to not implement that for now.
      specification.propagateChanges(viewChanges,
                                     correspondenceTranslationStrategy.createTranslatedCorrespondenceModelView(
                                         changedCorrespondenceModel, context),
                                     context.getResourceAccess(), null);

      targetView.commit();

      originChanges.forEach(change -> notifyChangePropagationStopped(this, change));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void propagateChange(
      EChange<EObject> eChange,
      EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView,
      ResourceAccess resourceAccess) {
    throw new UnsupportedOperationException(
        "This method should not be called, use propagateChanges instead");
  }

  @Override
  public void setUserInteractor(UserInteractor userInteractor) {
    super.setUserInteractor(userInteractor);
    specification.setUserInteractor(userInteractor);
  }
}
