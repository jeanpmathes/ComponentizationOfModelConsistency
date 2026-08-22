package tools.vitruv.compmodelcons.change;

import java.util.List;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceResolver;

/**
 * A view that is used during change propagation as part of a view-based change propagation
 * specification.
 */
public interface ChangePropagationView extends AutoCloseable {
  /**
   * Get the view resource access. This gives access to the view-side resources.
   *
   * @return the view resource access
   */
  ResourceAccess getViewResourceAccess();

  /**
   * Fit the view to a changed origin state, reached after applying the origin changes to the origin
   * this view is based on. At the same time, this determines the changes that need to be applied to
   * the view to get it from the current to the fitted, changed state.
   *
   * @param changedOrigin              the changed origin state
   * @param changedCorrespondenceModel the changed correspondence model
   * @param originChanges              the changes that created the changed origin state
   * @return the changes that were needed to change this view to the changed, fitted view state
   */
  List<EChange<EObject>> fitAndDetermineChanges(
      ResourceAccess changedOrigin,
      CorrespondenceModelAccess changedCorrespondenceModel,
      List<EChange<EObject>> originChanges);

  /**
   * Get the correspondence resolver for this view.
   *
   * @return the correspondence resolver
   */
  CorrespondenceResolver getCorrespondenceResolver();

  /**
   * Commit all changes that were made to this view to the origin.
   */
  void commit();
}
