package tools.vitruv.compmodelcons.views.parts;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.ObjectView;
import tools.vitruv.compmodelcons.views.ViewCorrespondences;

import java.util.Optional;

/**
 * A part of a bidirectional transformation, itself a bidirectional transformation as well.
 */
public interface Part {
    ObjectView get(ObjectView origin, ViewCorrespondences correspondences);

    void put(EChange<EObject> eChange);

    Optional<EChange<EObject>> getChange(EChange<EObject> change);
}
