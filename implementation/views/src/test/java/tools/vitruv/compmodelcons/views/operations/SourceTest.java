package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import javax.naming.OperationNotSupportedException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class SourceTest extends AbstractOperationTest {
    @Test
    public void testGetShouldReturnAllObjectsOfGivenType() {
        // Origin Setup
        EClass restaurantClass = (EClass) models.getPackage(Model.RESTAURANT).getEClassifier("Restaurant");
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
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Source operation = new Source(storeClass);

        // Pre-Action Get
        operation.get(context);

        // Pre-Action Change
        EObject created = viewType.getEFactoryInstance().create(emptyClass);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);

        // Action
        Optional<ObjectBinding> result = operation.put(change, ObjectBinding.ofViewObject(created), context);

        // Assertions
        assertTrue(result.isPresent());
        assertThrows(UnsupportedOperationException.class, () -> result.get().viewObject());
        assertEquals(1, result.get().originObjects().size());
        assertEquals(storeClass, result.get().originObjects().get(0).eClass());
        assertTrue(correspondences.correspond(result.get().originObjects(), created));
    }

    @Test
    public void testPutOfDeletionShouldRemoveCorrespondence() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Source operation = new Source(storeClass);

        // Pre-Action Get
        List<ObjectBinding> results = operation.get(context);

        // Pre-Action Change
        EObject deleted = emptyClass.getEPackage().getEFactoryInstance().create(emptyClass);
        correspondences.addCorrespondence(results.get(0).originObjects(), deleted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(deleted);

        // Action
        Optional<ObjectBinding> result = operation.put(change, results.get(0), context);

        // Assertions
        assertFalse(result.isPresent());
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
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Source operation = new Source(storeClass);

        // Pre-Action Get
        operation.get(context);

        // Pre-Action Change
        EObject inserted = viewType.getEFactoryInstance().create(emptyClass);
        context.getViewModel().getContents().add(inserted);
        int index = context.getViewModel().getContents().indexOf(inserted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createInsertRootChange(inserted, context.getViewModel(), index);

        // Action
        Optional<ObjectBinding> result = operation.put(change, ObjectBinding.ofOriginObject(otherStore), context);

        // Assertions
        assertTrue(result.isPresent());
        assertEquals(1, result.get().originObjects().size());
        assertEquals(otherStore, result.get().originObjects().get(0));
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
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Source operation = new Source(storeClass);

        // Pre-Action Get
        List<ObjectBinding> results = operation.get(context);

        // Pre-Action Change
        EObject removed = viewType.getEFactoryInstance().create(emptyClass);
        context.getViewModel().getContents().add(removed);
        int index = context.getViewModel().getContents().indexOf(removed);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveRootChange(removed, context.getViewModel(), index);

        // Action
        Optional<ObjectBinding> result = operation.put(change, results.get(0), context);

        // Assertions
        assertTrue(result.isPresent());
        assertEquals(1, result.get().originObjects().size());
        assertEquals(store, result.get().originObjects().get(0));
        assertEquals(results.get(0).originObjects().get(0), result.get().originObjects().get(0));
        assertEquals(stores.size() - 1, context.getOriginObjects(storeClass).size());
        assertFalse(context.getOriginObjects(storeClass).contains(store));
    }

    @Test
    public void testPufOfRootInsertionShouldInsertNonRootElementIntoOriginContainer() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass restaurantClass = (EClass) models.getPackage(Model.RESTAURANT).getEClassifier("Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);
        EObject restaurant = restaurantClass.getEPackage().getEFactoryInstance().create(restaurantClass);

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Source operation = new Source(restaurantClass);

        // Pre-Action Get
        operation.get(context);

        // Pre-Action Change
        EObject inserted = viewType.getEFactoryInstance().create(emptyClass);
        context.getViewModel().getContents().add(inserted);
        int index = context.getViewModel().getContents().indexOf(inserted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createInsertRootChange(inserted, context.getViewModel(), index);

        // Action
        Optional<ObjectBinding> result = operation.put(change, ObjectBinding.ofOriginObject(restaurant), context);

        // Assertions
        assertTrue(result.isPresent());
        assertEquals(1, result.get().originObjects().size());
        assertEquals(restaurant, result.get().originObjects().get(0));
        assertEquals(restaurants.size() + 1, context.getOriginObjects(restaurantClass).size());
        assertTrue(context.getOriginObjects(restaurantClass).contains(restaurant));
        assertFalse(context.getViewModel().getContents().contains(restaurant));
        assertTrue(store.eContents().contains(restaurant));
    }

    @Test
    public void testPutOfRootRemovalShouldRemoveNonRootElementFromOriginContainer() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass restaurantClass = (EClass) models.getPackage(Model.RESTAURANT).getEClassifier("Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

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
        Optional<ObjectBinding> result = operation.put(change, results.get(0), context);

        // Assertions
        assertTrue(result.isPresent());
        assertEquals(1, result.get().originObjects().size());
        assertEquals(results.get(0).originObjects().get(0), result.get().originObjects().get(0));
        assertEquals(restaurants.size() - 1, context.getOriginObjects(restaurantClass).size());
        assertFalse(context.getOriginObjects(restaurantClass).contains(result.get().originObjects().get(0)));
        assertFalse(store.eContents().contains(result.get().originObjects().get(0)));
    }
}
