package tools.vitruv.compmodelcons.views;

import org.eclipse.emf.ecore.EObject;

import java.util.List;

/**
 * Used to track correspondences between objects in the origin models and the objects of the view.
 * There are two reasons why this interface is used instead of the correspondence model:
 * <ol>
 *     <li>
 *         The correspondence model is not directional, this interface is.
 *     </li>
 *     <li>
 *         The correspondence model supports serialization, this explicitly does not; views are temporary.
 *     </li>
 * </ol>
 */
public interface ViewCorrespondences { // todo: evaluate whether correspondences are actually needed
    List<EObject> getCorrespondingViewObjectsForOriginObjects(List<EObject> originObjects);

    List<EObject> getCorrespondingOriginObjectsForViewObjects(List<EObject> viewObjects);

    boolean correspond(List<EObject> originObjects, EObject viewObjects);
}
