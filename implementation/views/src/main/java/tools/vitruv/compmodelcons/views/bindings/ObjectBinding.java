package tools.vitruv.compmodelcons.views.bindings;

import org.eclipse.emf.ecore.EObject;

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

    List<EObject> originObjects();

    EObject viewObject();
}
