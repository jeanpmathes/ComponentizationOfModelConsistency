package tools.vitruv.compmodelcons.views;

import org.eclipse.emf.ecore.EObject;

import java.util.List;

public interface EditableViewCorrespondences extends ViewCorrespondences {
    void addCorrespondence(List<EObject> originObjects, EObject viewObject);

    void removeCorrespondence(List<EObject> originObjects, EObject viewObject);
}
