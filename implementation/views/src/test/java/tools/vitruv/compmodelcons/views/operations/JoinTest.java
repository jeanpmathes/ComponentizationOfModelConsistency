package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.bindings.OriginBinding;
import tools.vitruv.compmodelcons.views.expressions.Condition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JoinTest extends AbstractOperationTest {
    @Test
    public void testGetShouldJoinAllObjectsOfGivenType() {
        // Origin Setup
        EClass restaurantClass = DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);
        List<OriginBinding> restaurantBindings = restaurants.stream().map(OriginBinding::of).toList();

        // Operation Setup
        OriginOperation originOperation = mock(OriginOperation.class);
        Join operation = new Join(restaurantClass, originOperation, Join.Type.INNER, Condition.TRUE);

        // Action
        when(originOperation.doGet(context)).thenReturn(restaurantBindings);
        List<OriginBinding> result = operation.doGet(context);

        // Assertions
        assertEquals(restaurants.size() * restaurants.size(), result.size());
        assertForAll(result, binding -> binding.originObjects().size() == 2);
        assertForAll(result, binding -> restaurants.contains(binding.originObjects().getFirst()));
        assertForAll(result, binding -> restaurants.contains(binding.originObjects().get(1)));
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
        Join operation = new Join(storeClass, originOperation, Join.Type.INNER, Condition.TRUE);

        // Pre-Action Get
        when(originOperation.doGet(context)).thenReturn(List.of(OriginBinding.of(restaurants.getFirst())));
        operation.doGet(context);

        // Pre-Action Change
        EObject created = DynamicModels.createEObject(emptyClass);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);

        // Action
        EObject createdRestaurant = DynamicModels.createEObject(restaurantClass);
        correspondences.addCorrespondence(List.of(createdRestaurant), created);
        when(originOperation.doPut(eq(change), any(), eq(context))).thenReturn(OriginBinding.of(createdRestaurant));
        OriginBinding result = operation.doPut(change, OriginBinding.empty(), context);

        // Assertions
        verify(originOperation, times(1)).doPut(eq(change), any(), eq(context));
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
        Join operation = new Join(storeClass, originOperation, Join.Type.INNER, Condition.TRUE);

        // Pre-Action Get
        when(originOperation.doGet(context)).thenReturn(List.of(OriginBinding.of(restaurants.getFirst())));
        List<OriginBinding> results = operation.doGet(context);

        // Pre-Action Change
        EObject deleted = DynamicModels.createEObject(emptyClass);
        correspondences.addCorrespondence(results.getFirst().originObjects(), deleted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(deleted);

        // Action
        when(originOperation.doPut(eq(change), any(), eq(context))).thenReturn(OriginBinding.empty());
        OriginBinding result = operation.doPut(change, results.getFirst(), context);

        // Assertions
        assertTrue(result.originObjects().isEmpty());
        assertTrue(correspondences.correspond(results.getFirst().originObjects().subList(0, 1), deleted));
        assertFalse(correspondences.correspond(results.getFirst().originObjects(), deleted));
    }
}