package tools.vitruv.compmodelcons.views.impl;

import tools.vitruv.change.atomic.eobject.impl.EObjectAddedEChangeImpl;
import tools.vitruv.compmodelcons.views.InsertNonRootEObject;

public class InsertNonRootEObjectImpl<Element> extends EObjectAddedEChangeImpl<Element> implements InsertNonRootEObject<Element> {
    public InsertNonRootEObjectImpl(Element newValue) {
        super();
        this.setNewValue(newValue);
    }
}
