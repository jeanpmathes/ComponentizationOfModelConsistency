package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class IntegratedOperationsTest extends AbstractOperationTest {
    @Test
    public void testStore2EmptyGet() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();
        List<EObject> stores = context.getOriginObjects(storeClass);

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Project operation = new Project(emptyClass, true, new Source(storeClass));

        // Action
        List<ObjectBinding> result = operation.get(context);

        // Assertions
        assertEquals(stores.size(), result.size());
        assertTrue(context.getViewModel().getContents().contains(result.get(0).viewObject()));
        assertForAll(result, binding -> binding.originObjects().size() == 1);
        assertForAll(result, binding -> stores.contains(binding.originObjects().get(0)));
        assertForAll(result, binding -> binding.viewObject().eClass() == emptyClass);
        assertForAll(result, binding -> isTrueForOne(stores, s -> correspondences.correspond(List.of(s), List.of(binding.viewObject()))));
    }

    @Test
    public void testRestaurant2EmptyGet() {
        // Origin Setup
        EClass restaurantClass = (EClass) models.getPackage(Model.RESTAURANT).getEClassifier("Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Project operation = new Project(emptyClass, false, new Source(restaurantClass));

        // Action
        List<ObjectBinding> result = operation.get(context);

        // Assertions
        assertEquals(restaurants.size(), result.size());
        assertForAll(result, binding -> binding.originObjects().size() == 1);
        assertForAll(result, binding -> restaurants.contains(binding.originObjects().get(0)));
        assertForAll(result, binding -> binding.viewObject().eClass() == emptyClass);
        assertForAll(result, binding -> isTrueForOne(restaurants, s -> correspondences.correspond(List.of(s), List.of(binding.viewObject()))));
    }

    @Test
    public void testRestaurant2EmptyPutCreate() {
        // Origin Setup
        EClass restaurantClass = (EClass) models.getPackage(Model.RESTAURANT).getEClassifier("Restaurant");

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Project operation = new Project(emptyClass, false, new Source(restaurantClass));

        // Pre-Action Get
        operation.get(context);

        // Pre-Action Change
        EObject created = viewType.getEFactoryInstance().create(emptyClass);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);

        // Action
        Optional<ObjectBinding> changed = operation.put(change, ObjectBinding.ofViewObject(created), context);

        // Assertions
        assertTrue(changed.isPresent());
        assertSame(created, changed.get().viewObject());
        assertEquals(1, changed.get().originObjects().size());
        assertEquals(restaurantClass, changed.get().originObjects().get(0).eClass());
        assertTrue(correspondences.correspond(changed.get().originObjects(), List.of(created)));
    }

    @Test
    public void testRestaurant2EmptyPutDelete() {
        // Origin Setup
        EClass restaurantClass = (EClass) models.getPackage(Model.RESTAURANT).getEClassifier("Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Project operation = new Project(emptyClass, true, new Source(restaurantClass));

        // Pre-Action Get
        List<ObjectBinding> result = operation.get(context);

        // Pre-Action Change
        EObject deleted = result.get(0).viewObject();
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(deleted);

        // Action
        Optional<ObjectBinding> changed = operation.put(change, result.get(0), context);

        // Assertions
        assertFalse(changed.isPresent());
        assertFalse(correspondences.correspond(result.get(0).originObjects(), List.of(deleted)));
    }

    @Test
    public void testStore2EmptyPutInsertRoot() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();
        List<EObject> stores = context.getOriginObjects(storeClass);

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Project operation = new Project(emptyClass, true, new Source(storeClass));

        // Pre-Action Get
        operation.get(context);

        // Pre-Action Change
        EObject inserted = viewType.getEFactoryInstance().create(emptyClass);
        ObjectBinding insertedBinding = addNewlyCreatedEObject(inserted, operation);
        context.getViewModel().getContents().add(inserted);
        int index = context.getViewModel().getContents().indexOf(inserted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createInsertRootChange(inserted, context.getViewModel(), index);

        // Action
        Optional<ObjectBinding> changed = operation.put(change, insertedBinding, context);

        // Assertions
        assertTrue(changed.isPresent());
        assertEquals(stores.size() + 1, context.getOriginObjects(storeClass).size());
        assertEquals(stores.size() + 1, models.getModel(Model.RESTAURANT).getContents().size());
        assertEquals(1, correspondences.getCorrespondingOriginObjectsForViewObjects(List.of(inserted)).size());
        assertTrue(context.getOriginObjects(storeClass).contains(correspondences.getCorrespondingOriginObjectsForViewObjects(List.of(inserted)).get(0)));
    }

    @Test
    public void testStore2EmptyPutRemoveRoot() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();
        List<EObject> stores = context.getOriginObjects(storeClass);

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Project operation = new Project(emptyClass, true, new Source(storeClass));

        // Pre-Action Get
        List<ObjectBinding> result = operation.get(context);

        // Pre-Action Change
        EObject removed = result.get(0).viewObject();
        int index = context.getViewModel().getContents().indexOf(removed);
        context.getViewModel().getContents().remove(removed);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveRootChange(removed, context.getViewModel(), index);

        // Action
        Optional<ObjectBinding> changed = operation.put(change, result.get(0), context);

        // Assertions
        assertTrue(changed.isPresent());
        assertEquals(stores.size() - 1, context.getOriginObjects(storeClass).size());
        assertEquals(stores.size() - 1, models.getModel(Model.RESTAURANT).getContents().size());
        assertTrue(Collections.disjoint(context.getOriginObjects(storeClass), result.get(0).originObjects()));
    }

    private ObjectBinding addNewlyCreatedEObject(EObject created, Operation operation) {
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);
        Optional<ObjectBinding> result = operation.put(change, ObjectBinding.ofViewObject(created), context);
        assertTrue(result.isPresent());
        return result.get();
    }
}
