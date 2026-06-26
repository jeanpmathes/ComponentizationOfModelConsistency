package tools.vitruv.compmodelcons.views.bindings;

import org.eclipse.emf.ecore.EObject;

import java.util.List;

public record ObjectBindingImpl(EObject viewObject, List<EObject> originObjects) implements ObjectBinding {
    public ObjectBindingImpl(EObject viewObject, List<EObject> originObjects) {
        this.viewObject = viewObject;
        this.originObjects = List.copyOf(originObjects);
    }
}
