package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JoinTest extends AbstractOperationTest {
    @Test
    public void testGetShouldJoinAllObjectsOfGivenType() {
        // Origin Setup
        EClass restaurantClass = DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);
        List<ObjectBinding> restaurantBindings = restaurants.stream().map(ObjectBinding::ofOriginObject).toList();

        // Operation Setup
        OriginOperation originOperation = mock(OriginOperation.class);
        Join operation = new Join(restaurantClass, originOperation);

        // Action
        when(originOperation.doGet(context)).thenReturn(restaurantBindings);
        List<ObjectBinding> result = operation.doGet(context);

        // Assertions
        assertEquals(restaurants.size() * restaurants.size(), result.size());
        assertForAll(result, binding -> binding.originObjects().size() == 2);
        assertForAll(result, binding -> restaurants.contains(binding.originObjects().get(0)));
        assertForAll(result, binding -> restaurants.contains(binding.originObjects().get(1)));
        result.forEach(binding -> assertThrows(UnsupportedOperationException.class, binding::viewObject));
    }

    @Test
    public void testPutOfCreationShouldCreateOriginObjectAndJoinCorrespondence() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();
        EClass restaurantClass = DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        OriginOperation originOperation = mock(OriginOperation.class);
        Join operation = new Join(storeClass, originOperation);

        // Pre-Action Get
        when(originOperation.doGet(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(restaurants.get(0))));
        operation.doGet(context);

        // Pre-Action Change
        EObject created = DynamicModels.createEObject(emptyClass);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);

        // Action
        EObject createdRestaurant = DynamicModels.createEObject(restaurantClass);
        correspondences.addCorrespondence(List.of(createdRestaurant), created);
        when(originOperation.doPut(eq(change), any(), eq(context))).thenReturn(ObjectBinding.ofOriginObject(createdRestaurant));
        ObjectBinding result = operation.doPut(change, ObjectBinding.ofViewObject(created), context);

        // Assertions
        verify(originOperation, times(1)).doPut(eq(change), any(), eq(context));
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertEquals(2, result.originObjects().size());
        assertEquals(createdRestaurant, result.originObjects().get(0));
        assertEquals(storeClass, result.originObjects().get(1).eClass());
        assertTrue(correspondences.correspond(result.originObjects(), created));
    }

    @Test
    public void testPutOfDeletionShouldReduceCorrespondence() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();
        EClass restaurantClass = DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        OriginOperation originOperation = mock(OriginOperation.class);
        Join operation = new Join(storeClass, originOperation);

        // Pre-Action Get
        when(originOperation.doGet(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(restaurants.get(0))));
        List<ObjectBinding> results = operation.doGet(context);

        // Pre-Action Change
        EObject deleted = DynamicModels.createEObject(emptyClass);
        correspondences.addCorrespondence(results.get(0).originObjects(), deleted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(deleted);

        // Action
        when(originOperation.doPut(eq(change), any(), eq(context))).thenReturn(ObjectBinding.empty());
        ObjectBinding result = operation.doPut(change, results.get(0), context);

        // Assertions
        assertThrows(UnsupportedOperationException.class, result::viewObject);
        assertTrue(result.originObjects().isEmpty());
        assertTrue(correspondences.correspond(results.get(0).originObjects().subList(0, 1), deleted));
        assertFalse(correspondences.correspond(results.get(0).originObjects(), deleted));
    }
}