package tools.vitruv.compmodelcons.views;

import tools.vitruv.change.atomic.eobject.EObjectSubtractedEChange;

/**
 * A removal of an EObject from a view model at any point that did not make the EObject a root object.
 * This means it was removed from a containing reference.
 *
 * @param <Element>
 */
public interface RemoveNonRootEObject<Element> extends EObjectSubtractedEChange<Element> {
}
