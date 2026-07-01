package tools.vitruv.compmodelcons.views.bindings;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.compmodelcons.views.ViewCorrespondences;

import java.util.List;

public interface ObjectBinding {
    static ObjectBinding ofOriginObject(EObject eObject) {
        return new ObjectBinding() {
            @Override
            public List<EObject> originObjects() {
                return List.of(eObject);
            }

            @Override
            public EObject viewObject() {
                throw new UnsupportedOperationException();
            }
        };
    }

    static ObjectBinding empty() {
        return new ObjectBinding() {
            @Override
            public List<EObject> originObjects() {
                return List.of();
            }

            @Override
            public EObject viewObject() {
                throw new UnsupportedOperationException();
            }
        };
    }

    static ObjectBinding ofViewObject(EObject eObject) {
        return new ObjectBinding() {
            @Override
            public List<EObject> originObjects() {
                return List.of();
            }

            @Override
            public EObject viewObject() {
                return eObject;
            }
        };
    }

    static ObjectBinding ofOriginObjects(List<EObject> originObjects, ViewCorrespondences correspondences) {
        var correspondingViewObjects = correspondences.getCorrespondingViewObjectsForOriginObjects(originObjects);
        if (correspondingViewObjects.size() != 1) {
            throw new IllegalArgumentException("Expected exactly one corresponding view object for origin objects");
        }
        return new ObjectBindingImpl(correspondingViewObjects.get(0), originObjects);
    }

    static ObjectBinding ofViewObject(EObject viewObject, ViewCorrespondences correspondences) {
        return new ObjectBindingImpl(viewObject, correspondences.getCorrespondingOriginObjectsForViewObjects(List.of(viewObject)));
    }

    List<EObject> originObjects();

    EObject viewObject();
}
