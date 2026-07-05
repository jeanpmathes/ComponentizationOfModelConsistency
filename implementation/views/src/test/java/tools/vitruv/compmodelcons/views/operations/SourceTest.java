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
import tools.vitruv.compmodelcons.views.impl.InsertNonRootEObjectImpl;
import tools.vitruv.compmodelcons.views.impl.RemoveNonRootEObjectImpl;

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
        List<ObjectBinding> result = source.get(context);

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
        operation.get(context);

        // Pre-Action Change
        EObject created = DynamicModels.createEObject(emptyClass);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);

        // Action
        ObjectBinding result = operation.put(change, ObjectBinding.ofViewObject(created), context);

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
        List<ObjectBinding> results = operation.get(context);

        // Pre-Action Change
        EObject deleted = DynamicModels.createEObject(emptyClass);
        correspondences.addCorrespondence(results.get(0).originObjects(), deleted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(deleted);

        // Action
        ObjectBinding result = operation.put(change, results.get(0), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertTrue(result.originObjects().isEmpty());
        assertFalse(correspondences.correspond(results.get(0).originObjects(), deleted));
    }

    @Test
    public void testPufOfRootInsertionShouldInsertRootElementIntoOriginModel() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();
        List<EObject> stores = context.getOriginObjects(storeClass);
        EObject otherStore = storeClass.getEPackage().getEFactoryInstance().create(storeClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        Source operation = new Source(storeClass);

        // Pre-Action Get
        operation.get(context);

        // Pre-Action Change
        EObject inserted = DynamicModels.createEObject(emptyClass);
        context.getViewModel().getContents().add(inserted);
        int index = context.getViewModel().getContents().indexOf(inserted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createInsertRootChange(inserted, context.getViewModel(), index);

        // Action
        ObjectBinding result = operation.put(change, ObjectBinding.ofOriginObject(otherStore), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertEquals(1, result.originObjects().size());
        assertEquals(otherStore, result.originObjects().get(0));
        assertEquals(stores.size() + 1, context.getOriginObjects(storeClass).size());
        assertTrue(context.getOriginObjects(storeClass).contains(otherStore));
    }

    @Test
    public void testPufOfNonRootInsertionShouldInsertRootElementIntoOriginModel() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();
        List<EObject> stores = context.getOriginObjects(storeClass);
        EObject otherStore = storeClass.getEPackage().getEFactoryInstance().create(storeClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);
        EClass emptyClass = DynamicModels.createEClass(viewType);
        EReference emptyContainment = DynamicModels.createContainmentEReference(rootClass, "containment", emptyClass);

        // View Setup
        EObject root = DynamicModels.createEObject(rootClass);

        // Operation Setup
        Source operation = new Source(storeClass);

        // Pre-Action Get
        operation.get(context);

        // Pre-Action Change
        EObject inserted = DynamicModels.createEObject(emptyClass);
        DynamicModels.getList(root, emptyContainment).add(inserted);
        EChange<EObject> change = new InsertNonRootEObjectImpl<>(inserted);

        // Action
        ObjectBinding result = operation.put(change, ObjectBinding.ofOriginObject(otherStore), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertEquals(1, result.originObjects().size());
        assertEquals(otherStore, result.originObjects().get(0));
        assertEquals(stores.size() + 1, context.getOriginObjects(storeClass).size());
        assertTrue(context.getOriginObjects(storeClass).contains(otherStore));
    }

    @Test
    public void testPutOfRootRemovalShouldRemoveRootElementFromOriginModel() {
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
        List<ObjectBinding> results = operation.get(context);

        // Pre-Action Change
        EObject removed = DynamicModels.createEObject(emptyClass);
        context.getViewModel().getContents().add(removed);
        int index = context.getViewModel().getContents().indexOf(removed);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveRootChange(removed, context.getViewModel(), index);

        // Action
        ObjectBinding result = operation.put(change, results.get(0), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertEquals(1, result.originObjects().size());
        assertEquals(store, result.originObjects().get(0));
        assertEquals(results.get(0).originObjects().get(0), result.originObjects().get(0));
        assertEquals(stores.size() - 1, context.getOriginObjects(storeClass).size());
        assertFalse(context.getOriginObjects(storeClass).contains(store));
    }

    @Test
    public void testPutOfNonRootRemovalShouldRemoveRootElementFromOriginModel() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();
        List<EObject> stores = context.getOriginObjects(storeClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);
        EClass emptyClass = DynamicModels.createEClass(viewType);
        EReference emptyContainment = DynamicModels.createContainmentEReference(rootClass, "containment", emptyClass);

        // View Setup
        EObject root = DynamicModels.createEObject(rootClass);

        // Operation Setup
        Source operation = new Source(storeClass);

        // Pre-Action Get
        List<ObjectBinding> results = operation.get(context);

        // Pre-Action Change
        EObject removed = DynamicModels.createEObject(emptyClass);
        DynamicModels.getList(root, emptyContainment).add(removed);
        EChange<EObject> change = new RemoveNonRootEObjectImpl<>(removed);

        // Action
        ObjectBinding result = operation.put(change, results.get(0), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertEquals(1, result.originObjects().size());
        assertEquals(store, result.originObjects().get(0));
        assertEquals(results.get(0).originObjects().get(0), result.originObjects().get(0));
        assertEquals(stores.size() - 1, context.getOriginObjects(storeClass).size());
        assertFalse(context.getOriginObjects(storeClass).contains(store));
    }

    @Test
    public void testPufOfRootInsertionShouldInsertNonRootElementIntoOriginContainer() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass restaurantClass = DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);
        EObject restaurant = DynamicModels.createEObject(restaurantClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        Source operation = new Source(restaurantClass);

        // Pre-Action Get
        operation.get(context);

        // Pre-Action Change
        EObject inserted = DynamicModels.createEObject(emptyClass);
        context.getViewModel().getContents().add(inserted);
        int index = context.getViewModel().getContents().indexOf(inserted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createInsertRootChange(inserted, context.getViewModel(), index);

        // Action
        ObjectBinding result = operation.put(change, ObjectBinding.ofOriginObject(restaurant), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertEquals(1, result.originObjects().size());
        assertEquals(restaurant, result.originObjects().get(0));
        assertEquals(restaurants.size() + 1, context.getOriginObjects(restaurantClass).size());
        assertTrue(context.getOriginObjects(restaurantClass).contains(restaurant));
        assertFalse(context.getViewModel().getContents().contains(restaurant));
        assertTrue(store.eContents().contains(restaurant));
    }

    @Test
    public void testPufOfNonRootInsertionShouldInsertNonRootElementIntoOriginContainer() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass restaurantClass = DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);
        EObject restaurant = DynamicModels.createEObject(restaurantClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType);
        EClass emptyClass = DynamicModels.createEClass(viewType);
        EReference emptyContainment = DynamicModels.createContainmentEReference(rootClass, "containment", emptyClass);

        // View Setup
        EObject root = DynamicModels.createEObject(rootClass);

        // Operation Setup
        Source operation = new Source(restaurantClass);

        // Pre-Action Get
        operation.get(context);

        // Pre-Action Change
        EObject inserted = DynamicModels.createEObject(emptyClass);
        DynamicModels.getList(root, emptyContainment).add(inserted);
        EChange<EObject> change = new InsertNonRootEObjectImpl<>(inserted);

        // Action
        ObjectBinding result = operation.put(change, ObjectBinding.ofOriginObject(restaurant), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertEquals(1, result.originObjects().size());
        assertEquals(restaurant, result.originObjects().get(0));
        assertEquals(restaurants.size() + 1, context.getOriginObjects(restaurantClass).size());
        assertTrue(context.getOriginObjects(restaurantClass).contains(restaurant));
        assertFalse(context.getViewModel().getContents().contains(restaurant));
        assertTrue(store.eContents().contains(restaurant));
    }

    @Test
    public void testPutOfRootRemovalShouldRemoveNonRootElementFromOriginContainer() {
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
        List<ObjectBinding> results = operation.get(context);

        // Pre-Action Change
        EObject removed = viewType.getEFactoryInstance().create(emptyClass);
        context.getViewModel().getContents().add(removed);
        int index = context.getViewModel().getContents().indexOf(removed);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveRootChange(removed, context.getViewModel(), index);

        // Action
        ObjectBinding result = operation.put(change, results.get(0), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertEquals(1, result.originObjects().size());
        assertEquals(results.get(0).originObjects().get(0), result.originObjects().get(0));
        assertEquals(restaurants.size() - 1, context.getOriginObjects(restaurantClass).size());
        assertFalse(context.getOriginObjects(restaurantClass).contains(result.originObjects().get(0)));
        assertFalse(store.eContents().contains(result.originObjects().get(0)));
    }

    @Test
    public void testPutOfNonRootRemovalShouldRemoveNonRootElementFromOriginContainer() {
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
        EObject root = DynamicModels.createEObject(rootClass);

        // Operation Setup
        Source operation = new Source(restaurantClass);

        // Pre-Action Get
        List<ObjectBinding> results = operation.get(context);

        // Pre-Action Change
        EObject removed = DynamicModels.createEObject(emptyClass);
        DynamicModels.getList(root, emptyContainment).add(removed);
        EChange<EObject> change = new RemoveNonRootEObjectImpl<>(removed);

        // Action
        ObjectBinding result = operation.put(change, results.get(0), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertEquals(1, result.originObjects().size());
        assertEquals(results.get(0).originObjects().get(0), result.originObjects().get(0));
        assertEquals(restaurants.size() - 1, context.getOriginObjects(restaurantClass).size());
        assertFalse(context.getOriginObjects(restaurantClass).contains(result.originObjects().get(0)));
        assertFalse(store.eContents().contains(result.originObjects().get(0)));
    }
}
