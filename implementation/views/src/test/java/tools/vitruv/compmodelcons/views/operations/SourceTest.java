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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SourceTest extends AbstractOperationTest {
    @Test
    public void testGetShouldReturnAllObjectsOfGivenType() {
        // Origin Setup
        EClass restaurantClass = DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);

        // Operation Setup
        Source source = new Source(restaurantClass);

        // Action
        List<ObjectBinding> result = source.doGet(context);

        // Assertions
        assertEquals(restaurants.size(), result.size());
        assertForAll(result, binding -> binding.originObjects().size() == 1);
        assertForAll(result, binding -> restaurants.contains(binding.originObjects().get(0)));
        result.forEach(binding -> assertThrows(UnsupportedOperationException.class, binding::viewObject));
    }

    @Test
    public void testPutOfCreationShouldCreateOriginObjectAndAddCorrespondence() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        Source operation = new Source(storeClass);

        // Pre-Action Get
        operation.doGet(context);

        // Pre-Action Change
        EObject created = DynamicModels.createEObject(emptyClass);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);

        // Action
        ObjectBinding result = operation.doPut(change, ObjectBinding.ofViewObject(created), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertEquals(1, result.originObjects().size());
        assertEquals(storeClass, result.originObjects().get(0).eClass());
        assertTrue(correspondences.correspond(result.originObjects(), created));
    }

    @Test
    public void testPutOfDeletionShouldRemoveCorrespondence() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        Source operation = new Source(storeClass);

        // Pre-Action Get
        List<ObjectBinding> results = operation.doGet(context);

        // Pre-Action Change
        EObject deleted = DynamicModels.createEObject(emptyClass);
        correspondences.addCorrespondence(results.get(0).originObjects(), deleted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(deleted);

        // Action
        ObjectBinding result = operation.doPut(change, results.get(0), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertTrue(result.originObjects().isEmpty());
        assertFalse(correspondences.correspond(results.get(0).originObjects(), deleted));
    }

    @Test
    public void testPufOfRootCreationShouldInsertRootElementIntoOriginModel() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();
        List<EObject> stores = context.getOriginObjects(storeClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        Source operation = new Source(storeClass);

        // Pre-Action Get
        operation.doGet(context);

        // Pre-Action Change
        EObject created = DynamicModels.createEObject(emptyClass);
        context.getViewModel().getContents().add(created);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);

        // Action
        ObjectBinding result = operation.doPut(change, ObjectBinding.ofViewObject(created), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        EObject otherStore = assertOneAdded(stores, context.getOriginObjects(storeClass));
        assertEquals(1, result.originObjects().size());
        assertEquals(otherStore, result.originObjects().get(0));
        assertEquals(stores.size() + 1, context.getOriginObjects(storeClass).size());
        assertTrue(context.getOriginObjects(storeClass).contains(otherStore));
    }

    @Test
    public void testPufOfNonRootCreationShouldInsertRootElementIntoOriginModel() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();
        List<EObject> stores = context.getOriginObjects(storeClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);
        EClass emptyClass = DynamicModels.createEClass(viewType);
        EReference emptyContainment = DynamicModels.createManyContainmentEReference(rootClass, "containment", emptyClass);

        // View Setup
        EObject root = DynamicModels.createEObject(rootClass);

        // Operation Setup
        Source operation = new Source(storeClass);

        // Pre-Action Get
        operation.doGet(context);

        // Pre-Action Change
        EObject created = DynamicModels.createEObject(emptyClass);
        DynamicModels.getList(root, emptyContainment).add(created);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);

        // Action
        ObjectBinding result = operation.doPut(change, ObjectBinding.ofViewObject(created), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        EObject otherStore = assertOneAdded(stores, context.getOriginObjects(storeClass));
        assertEquals(1, result.originObjects().size());
        assertEquals(otherStore, result.originObjects().get(0));
        assertEquals(stores.size() + 1, context.getOriginObjects(storeClass).size());
        assertTrue(context.getOriginObjects(storeClass).contains(otherStore));
    }

    @Test
    public void testPutOfRootDeletionShouldRemoveRootElementFromOriginModel() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();
        List<EObject> stores = context.getOriginObjects(storeClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        Source operation = new Source(storeClass);

        // Pre-Action Get
        List<ObjectBinding> results = operation.doGet(context);

        // Pre-Action Change
        EObject deleted = DynamicModels.createEObject(emptyClass);
        context.getViewModel().getContents().add(deleted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(deleted);

        // Action
        ObjectBinding result = operation.doPut(change, results.get(0), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertEquals(0, result.originObjects().size());
        assertEquals(stores.size() - 1, context.getOriginObjects(storeClass).size());
        assertFalse(context.getOriginObjects(storeClass).contains(store));
    }

    @Test
    public void testPutOfNonRootDeleteShouldRemoveRootElementFromOriginModel() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();
        List<EObject> stores = context.getOriginObjects(storeClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);
        EClass emptyClass = DynamicModels.createEClass(viewType);
        EReference emptyContainment = DynamicModels.createManyContainmentEReference(rootClass, "containment", emptyClass);

        // View Setup
        EObject root = DynamicModels.createEObject(rootClass);

        // Operation Setup
        Source operation = new Source(storeClass);

        // Pre-Action Get
        List<ObjectBinding> results = operation.doGet(context);

        // Pre-Action Change
        EObject deleted = DynamicModels.createEObject(emptyClass);
        DynamicModels.getList(root, emptyContainment).add(deleted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(deleted);

        // Action
        ObjectBinding result = operation.doPut(change, results.get(0), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertEquals(0, result.originObjects().size());
        assertEquals(stores.size() - 1, context.getOriginObjects(storeClass).size());
        assertFalse(context.getOriginObjects(storeClass).contains(store));
    }

    @Test
    public void testPufOfRootCreationShouldInsertNonRootElementIntoOriginContainer() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass restaurantClass = DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        Source operation = new Source(restaurantClass);

        // Pre-Action Get
        operation.doGet(context);

        // Pre-Action Change
        EObject created = DynamicModels.createEObject(emptyClass);
        context.getViewModel().getContents().add(created);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);

        // Action
        ObjectBinding result = operation.doPut(change, ObjectBinding.ofViewObject(created), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        EObject restaurant = assertOneAdded(restaurants, context.getOriginObjects(restaurantClass));
        assertEquals(1, result.originObjects().size());
        assertEquals(restaurant, result.originObjects().get(0));
        assertEquals(restaurants.size() + 1, context.getOriginObjects(restaurantClass).size());
        assertTrue(context.getOriginObjects(restaurantClass).contains(restaurant));
        assertFalse(context.getViewModel().getContents().contains(restaurant));
        assertTrue(store.eContents().contains(restaurant));
    }

    @Test
    public void testPufOfNonRootCreationShouldInsertNonRootElementIntoOriginContainer() {
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
        EObject root = DynamicModels.createEObject(rootClass);

        // Operation Setup
        Source operation = new Source(restaurantClass);

        // Pre-Action Get
        operation.doGet(context);

        // Pre-Action Change
        EObject created = DynamicModels.createEObject(emptyClass);
        DynamicModels.getList(root, emptyContainment).add(created);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);

        // Action
        ObjectBinding result = operation.doPut(change, ObjectBinding.ofViewObject(created), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        EObject restaurant = assertOneAdded(restaurants, context.getOriginObjects(restaurantClass));
        assertEquals(1, result.originObjects().size());
        assertEquals(restaurant, result.originObjects().get(0));
        assertEquals(restaurants.size() + 1, context.getOriginObjects(restaurantClass).size());
        assertTrue(context.getOriginObjects(restaurantClass).contains(restaurant));
        assertFalse(context.getViewModel().getContents().contains(restaurant));
        assertTrue(store.eContents().contains(restaurant));
    }

    @Test
    public void testPutOfRootDeletionShouldRemoveNonRootElementFromOriginContainer() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass restaurantClass = DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        Source operation = new Source(restaurantClass);

        // Pre-Action Get
        List<ObjectBinding> results = operation.doGet(context);

        // Pre-Action Change
        EObject deleted = viewType.getEFactoryInstance().create(emptyClass);
        context.getViewModel().getContents().add(deleted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(deleted);

        // Action
        ObjectBinding result = operation.doPut(change, results.get(0), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertEquals(0, result.originObjects().size());
        assertEquals(restaurants.size() - 1, context.getOriginObjects(restaurantClass).size());
        assertFalse(context.getOriginObjects(restaurantClass).contains(results.get(0).originObjects().get(0)));
        assertFalse(store.eContents().contains(results.get(0).originObjects().get(0)));
    }

    @Test
    public void testPutOfNonRootDeletionShouldRemoveNonRootElementFromOriginContainer() {
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
        EObject root = DynamicModels.createEObject(rootClass);

        // Operation Setup
        Source operation = new Source(restaurantClass);

        // Pre-Action Get
        List<ObjectBinding> results = operation.doGet(context);

        // Pre-Action Change
        EObject deleted = DynamicModels.createEObject(emptyClass);
        DynamicModels.getList(root, emptyContainment).add(deleted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(deleted);

        // Action
        ObjectBinding result = operation.doPut(change, results.get(0), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertEquals(0, result.originObjects().size());
        assertEquals(restaurants.size() - 1, context.getOriginObjects(restaurantClass).size());
        assertFalse(context.getOriginObjects(restaurantClass).contains(results.get(0).originObjects().get(0)));
        assertFalse(store.eContents().contains(results.get(0).originObjects().get(0)));
    }
}
