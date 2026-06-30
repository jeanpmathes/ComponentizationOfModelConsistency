package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.InsertNonRootEObject;
import tools.vitruv.compmodelcons.views.RemoveNonRootEObject;
import tools.vitruv.compmodelcons.views.Utilities;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RootTest extends AbstractOperationTest {
    @Test
    public void testGetShouldProvideAttachAllObjectsToRootOrTheirContainmentReference() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        List<EObject> restaurants = context.getOriginObjects(getEClass(models.getPackage(Model.RESTAURANT), "Restaurant"));

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass rootClass = createEClass(viewType);
        EClass emptyClass = createEClass(viewType);
        EReference emptyContainment = createContainmentEReference(rootClass, emptyClass);

        // View Setup
        ObjectBinding root = createBinding(store, createEObject(rootClass));
        List<ObjectBinding> empties = restaurants.stream().map(r -> createBinding(r, createEObject(emptyClass))).toList();

        // Operation Setup
        Operation rootOperation = mock(Operation.class);
        Operation emptyContainedOperation = mock(Operation.class);
        Root operation = new Root(rootClass, rootOperation, List.of(new Root.Contained(emptyContainment, emptyContainedOperation)));

        // Action
        when(rootOperation.get(context)).thenReturn(List.of(root));
        when(emptyContainedOperation.get(context)).thenReturn(empties);
        List<ObjectBinding> results = operation.get(context);

        // Assertions
        verify(rootOperation, times(1)).get(context);
        verify(emptyContainedOperation, times(1)).get(context);
        assertEquals(1, results.size());
        assertTrue(context.getViewModel().getContents().contains(results.get(0).viewObject()));
        assertEquals(restaurants.size(), Utilities.getList(results.get(0).viewObject(), emptyContainment).size());
        assertEquals(empties.stream().map(ObjectBinding::viewObject).collect(Collectors.toSet()), new HashSet<>(Utilities.getList(results.get(0).viewObject(), emptyContainment)));
    }

    @Test
    public void testPutOfRootRemovalShouldPropagateToRootOperation() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        List<EObject> restaurants = context.getOriginObjects(getEClass(models.getPackage(Model.RESTAURANT), "Restaurant"));

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass rootClass = createEClass(viewType);
        EClass emptyClass = createEClass(viewType);
        EReference emptyContainment = createContainmentEReference(rootClass, emptyClass);

        // View Setup
        ObjectBinding root = createBinding(store, createEObject(rootClass));
        List<ObjectBinding> empties = restaurants.stream().map(r -> createBinding(r, createEObject(emptyClass))).toList();

        // Operation Setup
        Operation rootOperation = mock(Operation.class);
        Operation emptyContainedOperation = mock(Operation.class);
        Root operation = new Root(rootClass, rootOperation, List.of(new Root.Contained(emptyContainment, emptyContainedOperation)));

        // Pre-Action Get
        when(rootOperation.get(context)).thenReturn(List.of(root));
        when(emptyContainedOperation.get(context)).thenReturn(empties);
        List<ObjectBinding> results = operation.get(context);

        // Pre-Action Change
        EObject removed = root.viewObject();
        int index = context.getViewModel().getContents().indexOf(removed);
        context.getViewModel().getContents().remove(removed);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveRootChange(removed, context.getViewModel(), index);

        // Action
        when(rootOperation.put(any(), any(), any())).thenReturn(Optional.empty());
        when(emptyContainedOperation.put(any(), any(), any())).thenReturn(Optional.empty());
        Optional<ObjectBinding> result = operation.put(change, results.get(0), context);

        // Assertions
        verify(rootOperation, times(1)).put(eq(change), any(), eq(context));
        verify(emptyContainedOperation, never()).put(eq(change), any(), eq(context));
        assertFalse(result.isPresent());
    }

    @Test
    public void testPutOfInsertReferenceChangeShouldPropagateAsInsertNonRootChangeToSpecificContainedOperation() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        List<EObject> restaurants = context.getOriginObjects(getEClass(models.getPackage(Model.RESTAURANT), "Restaurant"));

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass rootClass = createEClass(viewType);
        EClass emptyClass = createEClass(viewType);
        EReference emptyContainment = createContainmentEReference(rootClass, emptyClass);

        // View Setup
        ObjectBinding root = createBinding(store, createEObject(rootClass));
        List<ObjectBinding> empties = restaurants.stream().map(r -> createBinding(r, createEObject(emptyClass))).toList();

        // Operation Setup
        Operation rootOperation = mock(Operation.class);
        Operation emptyContainedOperation = mock(Operation.class);
        Root operation = new Root(rootClass, rootOperation, List.of(new Root.Contained(emptyContainment, emptyContainedOperation)));

        // Pre-Action Get
        when(rootOperation.get(context)).thenReturn(List.of(root));
        when(emptyContainedOperation.get(context)).thenReturn(empties);
        List<ObjectBinding> results = operation.get(context);

        // Pre-Action Change
        EObject inserted = createEObject(emptyClass);
        Utilities.getList(root.viewObject(), emptyContainment).add(inserted);
        int index = Utilities.getList(root.viewObject(), emptyContainment).indexOf(inserted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createInsertReferenceChange(root.viewObject(), emptyContainment, inserted, index);

        // Action
        when(rootOperation.put(any(), any(), any())).thenReturn(Optional.empty());
        when(emptyContainedOperation.put(any(), any(), any())).thenReturn(Optional.empty());
        Optional<ObjectBinding> result = operation.put(change, results.get(0), context);

        // Assertions
        ArgumentMatcher<EChange<EObject>> matcher = arg -> {
            if (arg instanceof InsertNonRootEObject) {
                return ((InsertNonRootEObject<EObject>) arg).getNewValue().equals(inserted);
            }
            return false;
        };
        verify(rootOperation, never()).put(argThat(matcher), any(), eq(context));
        verify(emptyContainedOperation, times(1)).put(argThat(matcher), any(), eq(context));
        assertFalse(result.isPresent());
    }

    @Test
    public void testPutOfRemoveReferenceChangeShouldPropagateAsRemoveNonRootChangeToSpecificContainedOperation() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        List<EObject> restaurants = context.getOriginObjects(getEClass(models.getPackage(Model.RESTAURANT), "Restaurant"));

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass rootClass = createEClass(viewType);
        EClass emptyClass = createEClass(viewType);
        EReference emptyContainment = createContainmentEReference(rootClass, emptyClass);

        // View Setup
        ObjectBinding root = createBinding(store, createEObject(rootClass));
        List<ObjectBinding> empties = restaurants.stream().map(r -> createBinding(r, createEObject(emptyClass))).toList();

        // Operation Setup
        Operation rootOperation = mock(Operation.class);
        Operation emptyContainedOperation = mock(Operation.class);
        Root operation = new Root(rootClass, rootOperation, List.of(new Root.Contained(emptyContainment, emptyContainedOperation)));

        // Pre-Action Get
        when(rootOperation.get(context)).thenReturn(List.of(root));
        when(emptyContainedOperation.get(context)).thenReturn(empties);
        List<ObjectBinding> results = operation.get(context);

        // Pre-Action Change
        EObject removed = empties.get(0).viewObject();
        int index = Utilities.getList(root.viewObject(), emptyContainment).indexOf(removed);
        Utilities.getList(root.viewObject(), emptyContainment).remove(removed);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveReferenceChange(root.viewObject(), emptyContainment, removed, index);

        // Action
        when(rootOperation.put(any(), any(), any())).thenReturn(Optional.empty());
        when(emptyContainedOperation.put(any(), any(), any())).thenReturn(Optional.empty());
        Optional<ObjectBinding> result = operation.put(change, results.get(0), context);

        // Assertions
        ArgumentMatcher<EChange<EObject>> matcher = arg -> {
            if (arg instanceof RemoveNonRootEObject<EObject>) {
                return ((RemoveNonRootEObject<EObject>) arg).getOldValue().equals(removed);
            }
            return false;
        };
        verify(rootOperation, never()).put(argThat(matcher), any(), eq(context));
        verify(emptyContainedOperation, times(1)).put(argThat(matcher), any(), eq(context));
        assertFalse(result.isPresent());
    }
}
