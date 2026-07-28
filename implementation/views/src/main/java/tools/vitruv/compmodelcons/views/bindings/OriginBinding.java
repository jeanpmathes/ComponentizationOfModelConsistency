package tools.vitruv.compmodelcons.views.bindings;

import org.eclipse.emf.ecore.EObject;

import java.util.List;

public interface OriginBinding {
    static OriginBinding of(EObject eObject) {
        List<EObject> list = List.of(eObject);
        return () -> list;
    }

    static OriginBinding of(List<EObject> eObjects) {
        List<EObject> list = List.copyOf(eObjects);
        return () -> list;
    }

    static OriginBinding empty() {
        return List::of;
    }

    List<EObject> originObjects();
}
