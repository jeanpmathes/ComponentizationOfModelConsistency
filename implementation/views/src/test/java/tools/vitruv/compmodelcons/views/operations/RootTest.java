package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RootTest extends AbstractOperationTest {
    @Test
    public void testGetShouldProvideAndAttachAllObjectsToRootOrTheirContainmentReference() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        List<EObject> restaurants = context.getOriginObjects(DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant"));

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);
        EClass emptyClass = DynamicModels.createEClass(viewType);
        EReference emptyContainment = DynamicModels.createContainmentEReference(rootClass, "containment", emptyClass);

        // View Setup
        ObjectBinding root = createBinding(store, DynamicModels.createEObject(rootClass));
        List<ObjectBinding> empties = restaurants.stream().map(r -> createBinding(r, DynamicModels.createEObject(emptyClass))).toList();

        // Operation Setup
        Project rootProject = mock(Project.class);
        Project emptyProject = mock(Project.class);
        Root operation = new Root(rootClass, Optional.of(rootProject), List.of(new Root.Target(emptyContainment, emptyProject)));

        // Action
        when(rootProject.beginGetByCreatingViewObjects(context)).thenReturn(List.of(root));
        when(emptyProject.beginGetByCreatingViewObjects(context)).thenReturn(empties);
        List<ObjectBinding> results = operation.GET(context);

        // Assertions
        verify(rootProject, times(1)).beginGetByCreatingViewObjects(context);
        verify(rootProject, times(1)).completeGetByCallingGetOnFeatures(root, context);
        verify(emptyProject, times(1)).beginGetByCreatingViewObjects(context);
        verify(emptyProject, times(empties.size())).completeGetByCallingGetOnFeatures(any(), eq(context));
        assertEquals(1, results.size());
        assertTrue(context.getViewModel().getContents().contains(results.get(0).viewObject()));
        assertEquals(restaurants.size(), DynamicModels.getList(results.get(0).viewObject(), emptyContainment).size());
        assertEquals(empties.stream().map(ObjectBinding::viewObject).collect(Collectors.toSet()), new HashSet<>(DynamicModels.getList(results.get(0).viewObject(), emptyContainment)));
    }

    @Test
    public void testGetShouldProvideAndAttachAllObjectsToRootOrTheirContainmentReferenceWhenUsingEmptyRoot() {
        // Origin Setup
        List<EObject> restaurants = context.getOriginObjects(DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant"));

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);
        EClass emptyClass = DynamicModels.createEClass(viewType);
        EReference emptyContainment = DynamicModels.createContainmentEReference(rootClass, "containment", emptyClass);

        // View Setup
        List<ObjectBinding> empties = restaurants.stream().map(r -> createBinding(r, DynamicModels.createEObject(emptyClass))).toList();

        // Operation Setup
        Project emptyProject = mock(Project.class);
        Root operation = new Root(rootClass, Optional.empty(), List.of(new Root.Target(emptyContainment, emptyProject)));

        // Action
        when(emptyProject.beginGetByCreatingViewObjects(context)).thenReturn(empties);
        List<ObjectBinding> results = operation.GET(context);

        // Assertions
        verify(emptyProject, times(1)).beginGetByCreatingViewObjects(context);
        verify(emptyProject, times(empties.size())).completeGetByCallingGetOnFeatures(any(), eq(context));
        assertEquals(1, results.size());
        assertTrue(results.get(0).originObjects().isEmpty());
        assertTrue(context.getViewModel().getContents().contains(results.get(0).viewObject()));
        assertEquals(restaurants.size(), DynamicModels.getList(results.get(0).viewObject(), emptyContainment).size());
        assertEquals(empties.stream().map(ObjectBinding::viewObject).collect(Collectors.toSet()), new HashSet<>(DynamicModels.getList(results.get(0).viewObject(), emptyContainment)));
    }

    @Test
    public void testPutOfRootRemovalNotPropagate() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        List<EObject> restaurants = context.getOriginObjects(DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant"));

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);
        EClass emptyClass = DynamicModels.createEClass(viewType);
        EReference emptyContainment = DynamicModels.createContainmentEReference(rootClass, "containment", emptyClass);

        // View Setup
        ObjectBinding root = createBinding(store, DynamicModels.createEObject(rootClass));
        List<ObjectBinding> empties = restaurants.stream().map(r -> createBinding(r, DynamicModels.createEObject(emptyClass))).toList();

        // Operation Setup
        Project rootProject = mock(Project.class);
        Project emptyProject = mock(Project.class);
        Root operation = new Root(rootClass, Optional.of(rootProject), List.of(new Root.Target(emptyContainment, emptyProject)));

        // Pre-Action Get
        when(rootProject.beginGetByCreatingViewObjects(context)).thenReturn(List.of(root));
        when(emptyProject.beginGetByCreatingViewObjects(context)).thenReturn(empties);
        List<ObjectBinding> results = operation.GET(context);

        // Pre-Action Change
        EObject removed = root.viewObject();
        int index = context.getViewModel().getContents().indexOf(removed);
        context.getViewModel().getContents().remove(removed);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveRootChange(removed, context.getViewModel(), index);

        // Action
        when(rootProject.doPut(any(), any(), any())).thenReturn(root);
        operation.PUT(change, results.get(0), context);

        // Assertions
        verify(rootProject, never()).doPut(eq(change), any(), eq(context));
        verify(emptyProject, never()).doPut(eq(change), any(), eq(context));
    }

    @Test
    public void testPutOfChangeOfEmptyRootShouldFail() {
        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        Root operation = new Root(rootClass, Optional.empty(), List.of());

        // Pre-Action Get
        List<ObjectBinding> results = operation.GET(context);

        // Pre-Action Change
        EObject removed = results.get(0).viewObject();
        int index = context.getViewModel().getContents().indexOf(removed);
        context.getViewModel().getContents().remove(removed);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveRootChange(removed, context.getViewModel(), index);

        // Action & Assertions
        assertThrows(IllegalArgumentException.class, () -> operation.PUT(change, results.get(0), context));
    }

    @Test
    public void testPutOfInsertReferenceChangeShouldPropagateAsInsertNonRootChangeToSpecificContainedOperation() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass restaurantClass = DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);
        EClass emptyClass = DynamicModels.createEClass(viewType);
        EReference emptyContainment = DynamicModels.createContainmentEReference(rootClass, "containment", emptyClass);

        // View Setup
        ObjectBinding root = createBinding(store, DynamicModels.createEObject(rootClass));
        List<ObjectBinding> empties = restaurants.stream().map(r -> createBinding(r, DynamicModels.createEObject(emptyClass))).toList();

        // Operation Setup
        Project rootOperation = mock(Project.class);
        Project emptyContainedOperation = mock(Project.class);
        Root operation = new Root(rootClass, Optional.of(rootOperation), List.of(new Root.Target(emptyContainment, emptyContainedOperation)));

        // Pre-Action Get
        when(rootOperation.beginGetByCreatingViewObjects(context)).thenReturn(List.of(root));
        when(emptyContainedOperation.beginGetByCreatingViewObjects(context)).thenReturn(empties);
        List<ObjectBinding> results = operation.GET(context);

        // Pre-Action Change
        EObject inserted = DynamicModels.createEObject(emptyClass);
        EObject insertedCorrespondence = DynamicModels.createEObject(restaurantClass);
        DynamicModels.getList(root.viewObject(), emptyContainment).add(inserted);
        int index = DynamicModels.getList(root.viewObject(), emptyContainment).indexOf(inserted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createInsertReferenceChange(root.viewObject(), emptyContainment, inserted, index);

        // Action
        when(emptyContainedOperation.doPut(any(), any(), any())).thenReturn(createBinding(insertedCorrespondence, inserted));
        ObjectBinding result = operation.PUT(change, results.get(0), context);

        // Assertions
        verify(rootOperation, never()).doPut(any(), any(), eq(context));
        assertEquals(results.get(0).originObjects(), result.originObjects());
    }

    @Test
    public void testPutOfRemoveReferenceChangeShouldPropagateAsRemoveNonRootChangeToSpecificContainedOperation() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        List<EObject> restaurants = context.getOriginObjects(DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant"));

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);
        EClass emptyClass = DynamicModels.createEClass(viewType);
        EReference emptyContainment = DynamicModels.createContainmentEReference(rootClass, "containment", emptyClass);

        // View Setup
        ObjectBinding root = createBinding(store, DynamicModels.createEObject(rootClass));
        List<ObjectBinding> empties = restaurants.stream().map(r -> createBinding(r, DynamicModels.createEObject(emptyClass))).toList();

        // Operation Setup
        Project rootOperation = mock(Project.class);
        Project emptyContainedOperation = mock(Project.class);
        Root operation = new Root(rootClass, Optional.of(rootOperation), List.of(new Root.Target(emptyContainment, emptyContainedOperation)));

        // Pre-Action Get
        when(rootOperation.beginGetByCreatingViewObjects(context)).thenReturn(List.of(root));
        when(emptyContainedOperation.beginGetByCreatingViewObjects(context)).thenReturn(empties);
        List<ObjectBinding> results = operation.GET(context);

        // Pre-Action Change
        EObject removed = empties.get(0).viewObject();
        int index = DynamicModels.getList(root.viewObject(), emptyContainment).indexOf(removed);
        DynamicModels.getList(root.viewObject(), emptyContainment).remove(removed);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveReferenceChange(root.viewObject(), emptyContainment, removed, index);

        // Action
        when(emptyContainedOperation.doPut(any(), any(), any())).thenReturn(empties.get(0));
        ObjectBinding result = operation.PUT(change, results.get(0), context);

        // Assertions
        verify(rootOperation, never()).doPut(any(), any(), eq(context));
        assertEquals(results.get(0).originObjects(), result.originObjects());
    }

    @Test
    public void testPutOfDeleteChangeShouldPropagateAsDeleteChangeToRootOperation() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        List<EObject> restaurants = context.getOriginObjects(DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant"));

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);
        EClass emptyClass = DynamicModels.createEClass(viewType);
        EReference emptyContainment = DynamicModels.createContainmentEReference(rootClass, "containment", emptyClass);

        // View Setup
        ObjectBinding root = createBinding(store, DynamicModels.createEObject(rootClass));
        List<ObjectBinding> empties = restaurants.stream().map(r -> createBinding(r, DynamicModels.createEObject(emptyClass))).toList();

        // Operation Setup
        Project rootProject = mock(Project.class);
        Project emptyProject = mock(Project.class);
        Root operation = new Root(rootClass, Optional.of(rootProject), List.of(new Root.Target(emptyContainment, emptyProject)));

        // Pre-Action Get
        when(rootProject.beginGetByCreatingViewObjects(context)).thenReturn(List.of(root));
        when(emptyProject.beginGetByCreatingViewObjects(context)).thenReturn(empties);
        List<ObjectBinding> results = operation.GET(context);

        // Pre-Action Change
        EObject deleted = root.viewObject();
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(deleted);

        // Action
        when(rootProject.doPut(any(), any(), any())).thenReturn(root);
        ObjectBinding result = operation.PUT(change, results.get(0), context);

        // Assertions
        verify(rootProject, times(1)).doPut(eq(change), any(), eq(context));
        verify(emptyProject, never()).doPut(eq(change), any(), eq(context));
        assertEquals(results.get(0).originObjects(), result.originObjects());
    }

    @Test
    public void testPutOfDeleteChangeShouldPropagateAsDeleteChangeToSpecificContainedOperation() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        List<EObject> restaurants = context.getOriginObjects(DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant"));

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);
        EClass emptyClass = DynamicModels.createEClass(viewType);
        EReference emptyContainment = DynamicModels.createContainmentEReference(rootClass, "containment", emptyClass);

        // View Setup
        ObjectBinding root = createBinding(store, DynamicModels.createEObject(rootClass));
        List<ObjectBinding> empties = restaurants.stream().map(r -> createBinding(r, DynamicModels.createEObject(emptyClass))).toList();

        // Operation Setup
        Project rootOperation = mock(Project.class);
        Project emptyContainedOperation = mock(Project.class);
        Root operation = new Root(rootClass, Optional.of(rootOperation), List.of(new Root.Target(emptyContainment, emptyContainedOperation)));

        // Pre-Action Get
        when(rootOperation.beginGetByCreatingViewObjects(context)).thenReturn(List.of(root));
        when(emptyContainedOperation.beginGetByCreatingViewObjects(context)).thenReturn(empties);
        List<ObjectBinding> results = operation.GET(context);

        // Pre-Action Change
        EObject deleted = empties.get(0).viewObject();
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(deleted);

        // Action
        when(emptyContainedOperation.doPut(any(), any(), any())).thenReturn(ObjectBinding.empty());
        ObjectBinding result = operation.PUT(change, results.get(0), context);

        // Assertions
        verify(rootOperation, never()).doPut(any(), any(), eq(context));
        verify(emptyContainedOperation, times(1)).doPut(eq(change), any(), eq(context));
        assertEquals(results.get(0).originObjects(), result.originObjects());
    }
}
