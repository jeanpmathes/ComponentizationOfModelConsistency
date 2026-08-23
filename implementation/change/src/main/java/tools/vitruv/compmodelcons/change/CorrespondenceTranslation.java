package tools.vitruv.compmodelcons.change;

/**
 * View elements cannot directly be stored in the correspondence model because views are temporary.
 * This means that the elements within are temporary as well. When the correspondence model is
 * persisted, all correspondences to no longer valid view elements are automatically removed.
 * Therefore, view-based change propagation requires that correspondences are translated.
 */
public enum CorrespondenceTranslation {
  /**
   * The view elements are replaced with
   * {@link tools.vitruv.compmodelcons.change.viewid.model.ViewId} elements which are stored in a
   * {@link tools.vitruv.compmodelcons.change.viewid.model.ViewIdModel}. The elements store the
   * hierarchical ID of a specific view element, and the model storing them is permanent so that
   * the elements can be used in correspondences. The model is stored as consistency metadata.
   */
  VIEW_ID,

  /**
   * The view elements are not translated and directly used. While not generally useful, this can,
   * for example, be necessary when nesting multiple view-based change propagation specifications.
   */
  NONE
}
