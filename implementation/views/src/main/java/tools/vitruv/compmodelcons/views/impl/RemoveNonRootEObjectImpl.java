package tools.vitruv.compmodelcons.views.impl;

import tools.vitruv.change.atomic.eobject.impl.EObjectSubtractedEChangeImpl;
import tools.vitruv.compmodelcons.views.RemoveNonRootEObject;

public class RemoveNonRootEObjectImpl<Element> extends EObjectSubtractedEChangeImpl<Element> implements RemoveNonRootEObject<Element> {
    public RemoveNonRootEObjectImpl(Element oldValue) {
        super();
        this.setOldValue(oldValue);
    }
}
