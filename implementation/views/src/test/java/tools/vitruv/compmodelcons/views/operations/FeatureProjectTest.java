package tools.vitruv.compmodelcons.views.operations;

import com.google.common.collect.Streams;
import org.eclipse.emf.ecore.*;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueUpdateBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeatureProjectTest extends AbstractOperationTest {
    @Test
    public void testShouldReturnReferenceItWasCreatedWith() {
        EPackage viewType = DynamicModels.createEPackage();
        EClass simpleClass = DynamicModels.createEClass(viewType);
        EAttribute numberAttribute = DynamicModels.createEAttribute(simpleClass, "number", EcorePackage.eINSTANCE.getEInt());

        FeatureOperation originOperation = mock(FeatureOperation.class);
        FeatureProject operation = new FeatureProject(Optional.empty(), numberAttribute, originOperation);

        assertEquals(numberAttribute, operation.getCreatedFeature());
    }

    @Test
    public void testGetShouldSetFeatureOnViewObject() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass simpleClass = DynamicModels.createEClass(viewType);
        EAttribute numberAttribute = DynamicModels.createEAttribute(simpleClass, "number", EcorePackage.eINSTANCE.getEInt());

        // View Setup
        EObject simple = DynamicModels.createEObject(simpleClass);
        ObjectBinding simpleBinding = createBinding(store, simple);

        // Operation Setup
        FeatureOperation originOperation = mock(FeatureOperation.class);
        FeatureProject operation = new FeatureProject(Optional.empty(), numberAttribute, originOperation);

        // Action
        when(originOperation.doGet(simpleBinding, context)).thenReturn(FeatureBinding.ofOriginObject(store, ValueBinding.of(42)));
        FeatureBinding result = operation.doGet(simpleBinding, context);

        // Assertions
        verify(originOperation, times(1)).doGet(simpleBinding, context);
        assertEquals(new ValueBinding.Single(42), result.value());
        assertTrue(simple.eIsSet(numberAttribute));
        assertEquals(42, simple.eGet(numberAttribute));
        assertEquals(simple, result.viewSubjectObject());
        assertEquals(1, result.originSubjectObjects().size());
        assertEquals(store, result.originSubjectObjects().get(0));
    }

    @Test
    public void testGetShouldUnsetFeatureOnViewObject() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass simpleClass = DynamicModels.createEClass(viewType);
        EAttribute numberAttribute = DynamicModels.createEAttribute(simpleClass, "number", EcorePackage.eINSTANCE.getEInt());

        // View Setup
        EObject simple = DynamicModels.createEObject(simpleClass);
        ObjectBinding simpleBinding = createBinding(store, simple);

        // Operation Setup
        FeatureOperation originOperation = mock(FeatureOperation.class);
        FeatureProject operation = new FeatureProject(Optional.empty(), numberAttribute, originOperation);

        // Action
        when(originOperation.doGet(simpleBinding, context)).thenReturn(FeatureBinding.ofOriginObject(store, new ValueBinding.Unset()));
        FeatureBinding result = operation.doGet(simpleBinding, context);

        // Assertions
        verify(originOperation, times(1)).doGet(simpleBinding, context);
        assertEquals(new ValueBinding.Unset(), result.value());
        assertFalse(simple.eIsSet(numberAttribute));
        assertEquals(simple, result.viewSubjectObject());
        assertEquals(1, result.originSubjectObjects().size());
        assertEquals(store, result.originSubjectObjects().get(0));
    }

    @Test
    public void testGetShouldFillFeatureOnViewObject() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass simpleClass = DynamicModels.createEClass(viewType);
        EAttribute numberAttribute = DynamicModels.createEAttribute(simpleClass, "number", EcorePackage.eINSTANCE.getEInt());
        numberAttribute.setUpperBound(-1);

        // View Setup
        EObject simple = DynamicModels.createEObject(simpleClass);
        ObjectBinding simpleBinding = createBinding(store, simple);

        // Operation Setup
        FeatureOperation originOperation = mock(FeatureOperation.class);
        FeatureProject operation = new FeatureProject(Optional.empty(), numberAttribute, originOperation);

        // Action
        when(originOperation.doGet(simpleBinding, context)).thenReturn(FeatureBinding.ofOriginObject(store, new ValueBinding.Many(List.of(1, 2, 3))));
        FeatureBinding result = operation.doGet(simpleBinding, context);

        // Assertions
        verify(originOperation, times(1)).doGet(simpleBinding, context);
        assertEquals(new ValueBinding.Many(List.of(1, 2, 3)), result.value());
        assertTrue(simple.eIsSet(numberAttribute));
        assertEquals(List.of(1, 2, 3), simple.eGet(numberAttribute));
        assertEquals(simple, result.viewSubjectObject());
        assertEquals(1, result.originSubjectObjects().size());
        assertEquals(store, result.originSubjectObjects().get(0));
    }

    @Test
    public void testGetOfReferenceShouldMapUsingCorrespondences() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EReference restaurantsReference = DynamicModels.getEReference(store.eClass(), "restaurants");
        List<EObject> restaurants = DynamicModels.getList(store, restaurantsReference);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass simpleClass = DynamicModels.createEClass(viewType);
        EClass mappedClass = DynamicModels.createEClass(viewType);
        EReference mappedReference = DynamicModels.createManyContainmentEReference(simpleClass, "contained", mappedClass);

        // View Setup
        EObject simple = DynamicModels.createEObject(simpleClass);
        ObjectBinding simpleBinding = createBinding(store, simple);
        List<EObject> mappedObjects = new ArrayList<>();
        for (EObject restaurant : restaurants) {
            EObject mapped = DynamicModels.createEObject(mappedClass);
            mappedObjects.add(mapped);
            context.getCorrespondences().addCorrespondence(List.of(restaurant), mapped);
        }

        // Operation Setup
        FeatureOperation originOperation = mock(FeatureOperation.class);
        FeatureProject operation = new FeatureProject(Optional.of(restaurantsReference), mappedReference, originOperation);

        // Action
        when(originOperation.doGet(simpleBinding, context)).thenReturn(FeatureBinding.ofOriginObject(store, new ValueBinding.Many(restaurants)));
        FeatureBinding result = operation.doGet(simpleBinding, context);

        // Assertions
        verify(originOperation, times(1)).doGet(simpleBinding, context);
        assertEquals(new ValueBinding.Many(mappedObjects), result.value());
        assertTrue(simple.eIsSet(mappedReference));
        assertEquals(mappedObjects, simple.eGet(mappedReference));
        assertEquals(simple, result.viewSubjectObject());
        assertEquals(1, result.originSubjectObjects().size());
        assertEquals(store, result.originSubjectObjects().get(0));
    }

    @Test
    public void testPutOfUnsetShouldPassUnsetUpdateBindingToInnerOperation() {
        // Origin Setup
        EPackage metamodel = models.getPackage(Model.RESTAURANT);
        EClass restaurantClass = DynamicModels.getEClass(metamodel, "Restaurant");
        EObject restaurant = context.getOriginObjects(restaurantClass).get(0);
        EStructuralFeature numEmployees = restaurantClass.getEStructuralFeature("numEmployees");

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass simpleClass = DynamicModels.createEClass(viewType);
        EAttribute numberAttribute = DynamicModels.createEAttribute(simpleClass, "number", EcorePackage.eINSTANCE.getEInt());

        // View Setup
        EObject simple = DynamicModels.createEObject(simpleClass);
        ObjectBinding simpleBinding = createBinding(restaurant, simple);

        // Operation Setup
        FeatureOperation originOperation = mock(FeatureOperation.class);
        FeatureProject operation = new FeatureProject(Optional.of(numEmployees), numberAttribute, originOperation);

        // Pre-Action Get
        when(originOperation.doGet(simpleBinding, context)).thenReturn(FeatureBinding.ofOriginObject(restaurant, ValueBinding.of(42)));
        FeatureBinding result = operation.doGet(simpleBinding, context);

        // Pre-Action Change
        simple.eUnset(numberAttribute);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createUnsetFeatureChange(simple, numberAttribute);

        // Action
        when(originOperation.doPut(any(), any(), any(), any(), any())).thenReturn(FeatureBinding.ofOriginObject(restaurant, ValueBinding.of(0)));
        result = operation.doPut(change, result, simpleBinding, context);

        // Assertions
        verify(originOperation, times(1)).doPut(eq(change), any(), eq(simpleBinding), eq(new ValueUpdateBinding.Unset()), eq(context));
        assertEquals(new ValueBinding.Unset(), result.value());
    }

    @Test
    public void testPutOfInsertShouldPassMappedInsertUpdateBindingToInnerOperation() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EReference restaurantsReference = DynamicModels.getEReference(store.eClass(), "restaurants");
        EClass restaurantClass = restaurantsReference.getEReferenceType();
        List<EObject> restaurants = DynamicModels.getList(store, restaurantsReference);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass simpleClass = DynamicModels.createEClass(viewType);
        EClass mappedClass = DynamicModels.createEClass(viewType);
        EReference mappedReference = DynamicModels.createManyContainmentEReference(simpleClass, "contained", mappedClass);

        // View Setup
        EObject simple = DynamicModels.createEObject(simpleClass);
        ObjectBinding simpleBinding = createBinding(store, simple);
        List<EObject> mappedObjects = new ArrayList<>();
        for (EObject restaurant : restaurants) {
            EObject mapped = DynamicModels.createEObject(mappedClass);
            mappedObjects.add(mapped);
            context.getCorrespondences().addCorrespondence(List.of(restaurant), mapped);
        }

        // Operation Setup
        FeatureOperation originOperation = mock(FeatureOperation.class);
        FeatureProject operation = new FeatureProject(Optional.of(restaurantsReference), mappedReference, originOperation);

        // Pre-Action Get
        when(originOperation.doGet(simpleBinding, context)).thenReturn(FeatureBinding.ofOriginObject(store, new ValueBinding.Many(restaurants)));
        FeatureBinding result = operation.doGet(simpleBinding, context);

        // Pre-Action Change
        EObject inserted = DynamicModels.createEObject(mappedClass);
        EObject insertedRestaurant = DynamicModels.createEObject(restaurantClass);
        context.getCorrespondences().addCorrespondence(List.of(insertedRestaurant), inserted);
        DynamicModels.getList(simple, mappedReference).add(inserted);
        int index = DynamicModels.getList(simple, mappedReference).indexOf(inserted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createInsertReferenceChange(simple, mappedReference, inserted, index);

        // Action
        when(originOperation.doPut(any(), any(), any(), any(), any())).thenReturn(FeatureBinding.ofOriginObject(store, new ValueBinding.Many(Streams.concat(restaurants.stream(), Stream.of(insertedRestaurant)).toList())));
        result = operation.doPut(change, result, simpleBinding, context);

        // Assertions
        verify(originOperation, times(1)).doPut(eq(change), any(), eq(simpleBinding), eq(new ValueUpdateBinding.Insert(insertedRestaurant, index)), eq(context));
        assertEquals(new ValueBinding.Many(Streams.concat(mappedObjects.stream(), Stream.of(inserted)).toList()), result.value());
    }

    @Test
    public void testPutShouldFailWhenNoSourceIsAvailable() {
        // Origin Setup
        EPackage metamodel = models.getPackage(Model.RESTAURANT);
        EClass restaurantClass = DynamicModels.getEClass(metamodel, "Restaurant");
        EObject restaurant = context.getOriginObjects(restaurantClass).get(0);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass simpleClass = DynamicModels.createEClass(viewType);
        EAttribute numberAttribute = DynamicModels.createEAttribute(simpleClass, "number", EcorePackage.eINSTANCE.getEInt());

        // View Setup
        EObject simple = DynamicModels.createEObject(simpleClass);
        ObjectBinding simpleBinding = createBinding(restaurant, simple);

        // Operation Setup
        FeatureOperation originOperation = mock(FeatureOperation.class);
        FeatureProject operation = new FeatureProject(Optional.empty(), numberAttribute, originOperation);

        // Pre-Action Get
        when(originOperation.doGet(simpleBinding, context)).thenReturn(FeatureBinding.ofOriginObject(restaurant, ValueBinding.of(42)));
        FeatureBinding result = operation.doGet(simpleBinding, context);

        // Pre-Action Change
        simple.eUnset(numberAttribute);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createUnsetFeatureChange(simple, numberAttribute);

        // Action & Assertions
        assertThrows(UnsupportedOperationException.class, () -> operation.doPut(change, result, simpleBinding, context));
    }
}