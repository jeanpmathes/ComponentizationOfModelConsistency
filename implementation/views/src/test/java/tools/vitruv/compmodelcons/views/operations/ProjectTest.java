package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProjectTest extends AbstractOperationTest {
    @Test
    public void testGetShouldCreateCorrespondenceAndAddRootViewObjectToViewResource() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Operation source = mock(Operation.class);
        when(source.get(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        Project operation = new Project(emptyClass, true, source);

        // Action
        List<ObjectBinding> result = operation.get(context);

        // Assertions
        verify(source, times(1)).get(context);
        assertEquals(1, result.size());
        assertEquals(result.get(0).viewObject().eClass(), emptyClass);
        assertTrue(context.getViewModel().getContents().contains(result.get(0).viewObject()));
        assertTrue(correspondences.correspond(List.of(store), result.get(0).viewObject()));
    }

    @Test
    public void testGetShouldCreateCorrespondenceButNotAddNonRootViewObjectToViewResource() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Operation source = mock(Operation.class);
        when(source.get(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        Project operation = new Project(emptyClass, false, source);

        // Action
        List<ObjectBinding> result = operation.get(context);

        // Assertions
        verify(source, times(1)).get(context);
        assertEquals(1, result.size());
        assertEquals(result.get(0).viewObject().eClass(), emptyClass);
        assertFalse(context.getViewModel().getContents().contains(result.get(0).viewObject()));
        assertTrue(correspondences.correspond(List.of(store), result.get(0).viewObject()));
    }

    @Test
    public void testPutWithNoOriginObjectShouldCallTheInnerOperation() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EObject otherStore = store.eClass().getEPackage().getEFactoryInstance().create(store.eClass());

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Operation inner = mock(Operation.class);
        Project operation = new Project(emptyClass, true, inner);

        // Pre-Action Get
        when(inner.get(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        operation.get(context);

        // Pre-Action Change
        EObject created = viewType.getEFactoryInstance().create(emptyClass);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);

        // Action
        when(inner.put(any(), any(), any())).thenReturn(Optional.of(ObjectBinding.ofOriginObject(otherStore)));
        Optional<ObjectBinding> result = operation.put(change, ObjectBinding.ofViewObject(created), context);

        // Assertions
        verify(inner, times(1)).put(eq(change), any(), eq(context));
        assertTrue(result.isPresent());
        assertEquals(created, result.get().viewObject());
        assertEquals(otherStore, result.get().originObjects().get(0));
    }

    @Test
    public void testPutWithOriginObjectShouldCallTheInnerOperation() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Operation inner = mock(Operation.class);
        Project operation = new Project(emptyClass, true, inner);

        // Pre-Action Get
        when(inner.get(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        List<ObjectBinding> results = operation.get(context);

        // Pre-Action Change
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(results.get(0).viewObject());

        // Action
        when(inner.put(any(), any(), any())).thenReturn(Optional.of(ObjectBinding.ofOriginObject(results.get(0).originObjects().get(0))));
        Optional<ObjectBinding> result = operation.put(change, results.get(0), context);

        // Assertions
        verify(inner, times(1)).put(eq(change), any(), eq(context));
        assertTrue(result.isPresent());
        assertEquals(results.get(0).viewObject(), result.get().viewObject());
        assertEquals(results.get(0).originObjects(), result.get().originObjects());
    }
}
