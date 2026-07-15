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
        EReference emptyContainment = DynamicModels.createManyContainmentEReference(rootClass, "containment", emptyClass);

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
        List<ObjectBinding> results = operation.doGet(context).getRootBindings();

        // Assertions
        verify(rootProject, times(1)).beginGetByCreatingViewObjects(context);
        verify(rootProject, times(1)).completeGetByCallingGetOnFeatures(root, context);
        verify(emptyProject, times(1)).beginGetByCreatingViewObjects(context);
        verify(emptyProject, times(empties.size())).completeGetByCallingGetOnFeatures(any(), eq(context));
        assertEquals(1, results.size());
        assertTrue(models.getViewModel().getContents().contains(results.getFirst().viewObject()));
        assertEquals(restaurants.size(), DynamicModels.getList(results.getFirst().viewObject(), emptyContainment).size());
        assertEquals(empties.stream().map(ObjectBinding::viewObject).collect(Collectors.toSet()), new HashSet<>(DynamicModels.getList(results.getFirst().viewObject(), emptyContainment)));
    }

    @Test
    public void testGetShouldProvideAndAttachAllObjectsToRootOrTheirContainmentReferenceWhenUsingEmptyRoot() {
        // Origin Setup
        List<EObject> restaurants = context.getOriginObjects(DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant"));

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);
        EClass emptyClass = DynamicModels.createEClass(viewType);
        EReference emptyContainment = DynamicModels.createManyContainmentEReference(rootClass, "containment", emptyClass);

        // View Setup
        List<ObjectBinding> empties = restaurants.stream().map(r -> createBinding(r, DynamicModels.createEObject(emptyClass))).toList();

        // Operation Setup
        Project emptyProject = mock(Project.class);
        Root operation = new Root(rootClass, Optional.empty(), List.of(new Root.Target(emptyContainment, emptyProject)));

        // Action
        when(emptyProject.beginGetByCreatingViewObjects(context)).thenReturn(empties);
        List<ObjectBinding> results = operation.doGet(context).getRootBindings();

        // Assertions
        verify(emptyProject, times(1)).beginGetByCreatingViewObjects(context);
        verify(emptyProject, times(empties.size())).completeGetByCallingGetOnFeatures(any(), eq(context));
        assertEquals(1, results.size());
        assertTrue(results.getFirst().originObjects().isEmpty());
        assertTrue(models.getViewModel().getContents().contains(results.getFirst().viewObject()));
        assertEquals(restaurants.size(), DynamicModels.getList(results.getFirst().viewObject(), emptyContainment).size());
        assertEquals(empties.stream().map(ObjectBinding::viewObject).collect(Collectors.toSet()), new HashSet<>(DynamicModels.getList(results.getFirst().viewObject(), emptyContainment)));
    }

    @Test
    public void testPutOfChangeOfEmptyRootShouldFail() {
        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        Root operation = new Root(rootClass, Optional.empty(), List.of());

        // Pre-Action Get
        Root.ViewBinding results = operation.doGet(context);

        // Pre-Action Change
        EObject removed = results.getRootBindings().getFirst().viewObject();
        int index = models.getViewModel().getContents().indexOf(removed);
        models.getViewModel().getContents().remove(removed);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveRootChange(removed, models.getViewModel(), index);

        // Action & Assertions
        assertThrows(IllegalArgumentException.class, () -> operation.doPut(change, results, context));
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
        EReference emptyContainment = DynamicModels.createManyContainmentEReference(rootClass, "containment", emptyClass);

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
        Root.ViewBinding results = operation.doGet(context);

        // Pre-Action Change
        EObject inserted = DynamicModels.createEObject(emptyClass);
        EObject insertedCorrespondence = DynamicModels.createEObject(restaurantClass);
        DynamicModels.getList(root.viewObject(), emptyContainment).add(inserted);
        int index = DynamicModels.getList(root.viewObject(), emptyContainment).indexOf(inserted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createInsertReferenceChange(root.viewObject(), emptyContainment, inserted, index);

        // Action
        when(emptyContainedOperation.doPut(any(), any(), any())).thenReturn(createBinding(insertedCorrespondence, inserted));
        Root.ViewBinding result = operation.doPut(change, results, context);

        // Assertions
        verify(rootOperation, never()).doPut(any(), any(), eq(context));
        assertEquals(results.getRootBindings().getFirst().originObjects(), result.getRootBindings().getFirst().originObjects());
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
        EReference emptyContainment = DynamicModels.createManyContainmentEReference(rootClass, "containment", emptyClass);

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
        Root.ViewBinding results = operation.doGet(context);

        // Pre-Action Change
        EObject removed = empties.getFirst().viewObject();
        int index = DynamicModels.getList(root.viewObject(), emptyContainment).indexOf(removed);
        DynamicModels.getList(root.viewObject(), emptyContainment).remove(removed);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveReferenceChange(root.viewObject(), emptyContainment, removed, index);

        // Action
        when(emptyContainedOperation.doPut(any(), any(), any())).thenReturn(empties.getFirst());
        Root.ViewBinding result = operation.doPut(change, results, context);

        // Assertions
        verify(rootOperation, never()).doPut(any(), any(), eq(context));
        assertEquals(results.getRootBindings().getFirst().originObjects(), result.getRootBindings().getFirst().originObjects());
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
        EReference emptyContainment = DynamicModels.createManyContainmentEReference(rootClass, "containment", emptyClass);

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
        Root.ViewBinding results = operation.doGet(context);

        // Pre-Action Change
        EObject deleted = root.viewObject();
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(deleted);

        // Action
        when(rootProject.doPut(any(), any(), any())).thenReturn(root);
        Root.ViewBinding result = operation.doPut(change, results, context);

        // Assertions
        verify(rootProject, times(1)).doPut(eq(change), any(), eq(context));
        verify(emptyProject, never()).doPut(eq(change), any(), eq(context));
        assertEquals(results.getRootBindings().getFirst().originObjects(), result.getRootBindings().getFirst().originObjects());
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
        EReference emptyContainment = DynamicModels.createManyContainmentEReference(rootClass, "containment", emptyClass);

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
        Root.ViewBinding results = operation.doGet(context);

        // Pre-Action Change
        EObject deleted = empties.getFirst().viewObject();
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(deleted);

        // Action
        when(emptyContainedOperation.doPut(any(), any(), any())).thenReturn(ObjectBinding.empty());
        Root.ViewBinding result = operation.doPut(change, results, context);

        // Assertions
        verify(rootOperation, never()).doPut(any(), any(), eq(context));
        verify(emptyContainedOperation, times(1)).doPut(eq(change), any(), eq(context));
        assertEquals(results.getRootBindings().getFirst().originObjects(), result.getRootBindings().getFirst().originObjects());
    }
}
