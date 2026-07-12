package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EditableViewCorrespondencesImplTest {
    private final EditableViewCorrespondencesImpl correspondences = new EditableViewCorrespondencesImpl();

    @Test
    public void testAddCorrespondence() {
        EObject originObject1 = EcoreFactory.eINSTANCE.createEObject();
        EObject originObject2 = EcoreFactory.eINSTANCE.createEObject();
        EObject viewObject = EcoreFactory.eINSTANCE.createEObject();

        correspondences.addCorrespondence(List.of(originObject1, originObject2), viewObject);

        assertTrue(correspondences.correspond(List.of(originObject1, originObject2), viewObject));
        assertFalse(correspondences.correspond(List.of(originObject1), viewObject));

        assertEquals(List.of(originObject1, originObject2), correspondences.getCorrespondingOriginObjectsForViewObject(viewObject));
        assertEquals(viewObject, correspondences.getCorrespondingViewObjectForOriginObjects(List.of(originObject1, originObject2), viewObject.eClass()));
        assertEquals(viewObject, correspondences.getCorrespondingViewObjectForPartialOriginObjects(originObject1, viewObject.eClass()));
        assertEquals(viewObject, correspondences.getCorrespondingViewObjectForPartialOriginObjects(originObject2, viewObject.eClass()));
    }

    @Test
    public void testJoinCorrespondence() {
        EObject originObject1 = EcoreFactory.eINSTANCE.createEObject();
        EObject originObject2 = EcoreFactory.eINSTANCE.createEObject();
        EObject viewObject = EcoreFactory.eINSTANCE.createEObject();

        correspondences.addCorrespondence(List.of(originObject1), viewObject);
        correspondences.joinCorrespondence(List.of(originObject1), List.of(originObject2), viewObject);

        assertTrue(correspondences.correspond(List.of(originObject1, originObject2), viewObject));
        assertFalse(correspondences.correspond(List.of(originObject1), viewObject));

        assertEquals(List.of(originObject1, originObject2), correspondences.getCorrespondingOriginObjectsForViewObject(viewObject));
        assertEquals(viewObject, correspondences.getCorrespondingViewObjectForOriginObjects(List.of(originObject1, originObject2), viewObject.eClass()));
        assertEquals(viewObject, correspondences.getCorrespondingViewObjectForPartialOriginObjects(originObject1, viewObject.eClass()));
        assertEquals(viewObject, correspondences.getCorrespondingViewObjectForPartialOriginObjects(originObject2, viewObject.eClass()));
    }

    @Test
    public void testUnjoinCorrespondence() {
        EObject originObject1 = EcoreFactory.eINSTANCE.createEObject();
        EObject originObject2 = EcoreFactory.eINSTANCE.createEObject();
        EObject viewObject = EcoreFactory.eINSTANCE.createEObject();

        correspondences.addCorrespondence(List.of(originObject1, originObject2), viewObject);
        correspondences.unjoinCorrespondence(List.of(originObject1, originObject2), List.of(originObject2), viewObject);

        assertTrue(correspondences.correspond(List.of(originObject1), viewObject));
        assertFalse(correspondences.correspond(List.of(originObject1, originObject2), viewObject));

        assertEquals(List.of(originObject1), correspondences.getCorrespondingOriginObjectsForViewObject(viewObject));
        assertEquals(viewObject, correspondences.getCorrespondingViewObjectForOriginObjects(List.of(originObject1), viewObject.eClass()));
        assertEquals(viewObject, correspondences.getCorrespondingViewObjectForPartialOriginObjects(originObject1, viewObject.eClass()));
    }
}