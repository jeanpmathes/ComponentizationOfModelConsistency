package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.compmodelcons.views.EditableViewCorrespondences;

import java.util.List;

public class EditableViewCorrespondencesImpl implements EditableViewCorrespondences {
    @Override
    public List<EObject> getCorrespondingViewObjectsForOriginObjects(List<EObject> list) {
        return List.of();
    }

    @Override
    public List<EObject> getCorrespondingOriginObjectsForViewObjects(List<EObject> list) {
        return List.of();
    }
}
