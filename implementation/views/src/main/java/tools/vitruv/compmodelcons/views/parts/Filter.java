package tools.vitruv.compmodelcons.views.parts;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.ObjectView;
import tools.vitruv.compmodelcons.views.ViewCorrespondences;

import java.util.Optional;

public class Filter implements Part {
    @Override
    public ObjectView get(ObjectView origin, ViewCorrespondences correspondences) {
        return null;
    }

    @Override
    public void put(EChange<EObject> eChange) {

    }

    @Override
    public Optional<EChange<EObject>> getChange(EChange<EObject> change) {
        return Optional.empty();
    }
}
