package tools.vitruv.compmodelcons.views.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.compmodelcons.views.EditableViewCorrespondences;

import java.util.List;

public class EditableViewCorrespondencesImpl implements EditableViewCorrespondences {
    private final BiMap<List<EObject>, List<EObject>> correspondences;

    public EditableViewCorrespondencesImpl() {
        correspondences = HashBiMap.create();
    }

    @Override
    public List<EObject> getCorrespondingViewObjectsForOriginObjects(List<EObject> originObjects) {
        return correspondences.get(originObjects);
    }

    @Override
    public List<EObject> getCorrespondingOriginObjectsForViewObjects(List<EObject> viewObjects) {
        return correspondences.inverse().get(viewObjects);
    }

    @Override
    public boolean correspond(List<EObject> originObjects, List<EObject> viewObjects) {
        return correspondences.containsKey(originObjects) && correspondences.get(originObjects).equals(viewObjects);
    }

    @Override
    public void addCorrespondence(List<EObject> originObjects, EObject viewObject) {
        correspondences.put(originObjects, List.of(viewObject));
    }

    @Override
    public void removeCorrespondence(List<EObject> originObjects, EObject viewObject) {
        if (!getCorrespondingViewObjectsForOriginObjects(originObjects).equals(List.of(viewObject))) {
            throw new IllegalArgumentException("The correspondence to remove does not exist");
        }

        correspondences.remove(originObjects);
    }
}
