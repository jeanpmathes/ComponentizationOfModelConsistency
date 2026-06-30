package tools.vitruv.compmodelcons.views;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.EObjectAddedEChange;
import tools.vitruv.change.atomic.eobject.EObjectExistenceEChange;
import tools.vitruv.change.atomic.eobject.EObjectSubtractedEChange;
import tools.vitruv.change.atomic.feature.FeatureEChange;

public class Utilities {
    private Utilities() {
    }

    public static EList<EObject> getList(EObject target, EReference reference) {
        //noinspection unchecked
        return (EList<EObject>) target.eGet(reference);
    }

    public static EObject getAffectedEObject(EChange<EObject> eChange) {
        if (eChange instanceof EObjectExistenceEChange<EObject> eObjectEObjectExistenceEChange) {
            return eObjectEObjectExistenceEChange.getAffectedElement();
        } else if (eChange instanceof FeatureEChange<EObject, ?> featureEChange) {
            return featureEChange.getAffectedElement();
        } else if (eChange instanceof EObjectAddedEChange<EObject> eObjectEObjectAddedEChange) {
            return eObjectEObjectAddedEChange.getNewValue();
        } else if (eChange instanceof EObjectSubtractedEChange<EObject> eObjectEObjectSubtractedEChange) {
            return eObjectEObjectSubtractedEChange.getOldValue();
        } else {
            throw new IllegalArgumentException("Unknown change type: " + eChange.getClass().getSimpleName());
        }
    }
}
