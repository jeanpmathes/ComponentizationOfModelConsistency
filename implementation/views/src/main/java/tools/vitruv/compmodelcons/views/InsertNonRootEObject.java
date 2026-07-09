package tools.vitruv.compmodelcons.views;

import tools.vitruv.change.atomic.eobject.EObjectAddedEChange;

/**
 * An insertion of an EObject into a view model at any point that does not make the EObject a root object.
 * This means it was inserted into a containing reference.
 *
 * @param <Element>
 */
public interface InsertNonRootEObject<Element> extends EObjectAddedEChange<Element> {
}
