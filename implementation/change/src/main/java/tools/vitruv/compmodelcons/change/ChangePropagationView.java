package tools.vitruv.compmodelcons.change;

import java.util.List;
import java.util.function.Function;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.propagation.ModelRepositorySnapshot;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceObjectViewObjectTranslator;
import tools.vitruv.compmodelcons.change.correspondence.ViewCorrespondences;

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
   * Create a snapshot of the current state of the view.
   *
   * @return the snapshot
   */
  ModelRepositorySnapshot createSnapshot();

  /**
   * Fit the view to a changed origin state, reached after applying the origin changes to the origin
   * this view is based on. At the same time, this determines the changes that need to be applied to
   * the view to get it from the current to the fitted, changed state.
   *
   * @param changedOrigin              the changed origin state
   * @param changedCorrespondenceModel the changed correspondence model
   * @param originChanges              the changes that created the changed origin state
   * @param unchangedToChanged         maps origin objects from the unchanged origin state to the
   *                                   respective origin objects in the changed origin state
   * @return the changes that were needed to change this view to the changed, fitted view state
   */
  List<EChange<EObject>> fitAndDetermineChanges(
      ResourceAccess changedOrigin,
      CorrespondenceModelAccess changedCorrespondenceModel,
      List<EChange<EObject>> originChanges,
      Function<EObject, EObject> unchangedToChanged);

  /**
   * Get the correspondence resolver for this view.
   *
   * @return the correspondence resolver
   */
  CorrespondenceObjectViewObjectTranslator getCorrespondenceResolver();

  /**
   * Get the view-correspondences of this view. View-correspondences connect view objects with
   * the origin objects the view elements were projected from.
   *
   * @return the view-correspondences
   */
  ViewCorrespondences getCorrespondences();

  /**
   * Commit all changes that were made to this view to the origin.
   */
  void commit();
}
