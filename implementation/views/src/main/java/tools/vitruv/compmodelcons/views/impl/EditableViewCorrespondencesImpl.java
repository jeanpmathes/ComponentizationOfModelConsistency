package tools.vitruv.compmodelcons.views.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.compmodelcons.views.EditableViewCorrespondences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditableViewCorrespondencesImpl implements EditableViewCorrespondences {
    private final BiMap<OriginKey, ViewKey> correspondences = HashBiMap.create();
    private final Map<PartialOriginKey, ViewKey> partialCorrespondences = new HashMap<>();

    public EditableViewCorrespondencesImpl() {

    }

    @Override
    public EObject getCorrespondingViewObjectForOriginObjects(List<EObject> originObjects, EClass viewClass) {
        return correspondences.get(new OriginKey(originObjects, viewClass)).viewObject();
    }

    @Override
    public List<EObject> getCorrespondingOriginObjectsForViewObject(EObject viewObject) {
        return correspondences.inverse().get(new ViewKey(viewObject)).originObjects();
    }

    @Override
    public EObject getCorrespondingViewObjectForPartialOriginObjects(EObject originObject, EClass viewClass) {
        return partialCorrespondences.get(new PartialOriginKey(originObject, viewClass)).viewObject();
    }

    @Override
    public boolean correspond(List<EObject> originObjects, EObject viewObject) {
        return correspondences.containsKey(new OriginKey(originObjects, viewObject.eClass())) && correspondences.get(new OriginKey(originObjects, viewObject.eClass())).equals(new ViewKey(viewObject));
    }

    @Override
    public void addCorrespondence(List<EObject> originObjects, EObject viewObject) {
        if (originObjects.isEmpty()) {
            return;
        }

        var viewKey = new ViewKey(viewObject);

        correspondences.put(new OriginKey(originObjects, viewObject.eClass()), viewKey);

        for (var originObject : originObjects) {
            partialCorrespondences.put(new PartialOriginKey(originObject, viewObject.eClass()), viewKey);
        }
    }

    @Override
    public void removeCorrespondence(List<EObject> originObjects, EObject viewObject) {
        correspondences.remove(new OriginKey(originObjects, viewObject.eClass()), new ViewKey(viewObject));
    }

    private record OriginKey(List<EObject> originObjects, EClass viewClass) {
    }

    private record ViewKey(EObject viewObject) {
    }

    private record PartialOriginKey(EObject originObject, EClass viewClass) {
    }
}
