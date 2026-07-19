package tools.vitruv.compmodelcons.views.operations;

import com.google.common.collect.Streams;
import org.eclipse.emf.ecore.*;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueUpdateBinding;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class FeatureSourceTest extends AbstractOperationTest {
    @Test
    public void testGetShouldGetFeatureOfOriginObject() {
        // Origin Setup
        EPackage metamodel = models.getPackage(Model.RESTAURANT);
        EClass restaurantClass = DynamicModels.getEClass(metamodel, "Restaurant");
        EObject restaurant = context.getOriginObjects(restaurantClass).get(0);
        EStructuralFeature numEmployees = restaurantClass.getEStructuralFeature("numEmployees");
        int numEmployeesValue = (int) restaurant.eGet(numEmployees);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass simpleClass = DynamicModels.createEClass(viewType);
        DynamicModels.createEAttribute(simpleClass, "number", EcorePackage.eINSTANCE.getEInt());

        // View Setup
        EObject simple = DynamicModels.createEObject(simpleClass);
        ObjectBinding simpleBinding = createBinding(restaurant, simple);

        // Operation Setup
        FeatureOriginOperation operation = new FeatureSource(numEmployees);

        // Action
        FeatureBinding result = operation.doGet(simpleBinding, context);

        // Assertions
        assertEquals(new ValueBinding.Single(numEmployeesValue), result.value());
        assertThrows(UnsupportedOperationException.class, result::viewSubjectObject);
        assertEquals(1, result.originSubjectObjects().size());
        assertEquals(restaurant, result.originSubjectObjects().get(0));
    }

    @Test
    public void testPutOfReplaceShouldReplaceFeatureOnOriginObject() {
        // Origin Setup
        EPackage metamodel = models.getPackage(Model.RESTAURANT);
        EClass restaurantClass = DynamicModels.getEClass(metamodel, "Restaurant");
        EObject restaurant = context.getOriginObjects(restaurantClass).get(0);
        EStructuralFeature numEmployees = restaurantClass.getEStructuralFeature("numEmployees");
        int numEmployeesValue = (int) restaurant.eGet(numEmployees);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass simpleClass = DynamicModels.createEClass(viewType);
        EAttribute numberAttribute = DynamicModels.createEAttribute(simpleClass, "number", EcorePackage.eINSTANCE.getEInt());

        // View Setup
        EObject simple = DynamicModels.createEObject(simpleClass);
        ObjectBinding simpleBinding = createBinding(restaurant, simple);

        // Operation Setup
        FeatureOriginOperation operation = new FeatureSource(numEmployees);

        // Pre-Action Get
        FeatureBinding result = operation.doGet(simpleBinding, context);

        // Pre-Action Change
        simple.eSet(numberAttribute, numEmployeesValue + 3);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createReplaceSingleAttributeChange(simple, numberAttribute, numEmployeesValue, numEmployeesValue + 3);

        // Action
        result = operation.doPut(change, result, simpleBinding, new ValueUpdateBinding.Replace(numEmployeesValue + 3), context);

        // Assertions
        assertEquals(new ValueBinding.Single(numEmployeesValue + 3), result.value());
        assertEquals(numEmployeesValue + 3, restaurant.eGet(numEmployees));
    }

    @Test
    public void testPutOfInsertShouldInsertFeatureOnOriginObject() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EReference restaurantsReference = DynamicModels.getEReference(store.eClass(), "restaurants");
        EClass restaurantClass = restaurantsReference.getEReferenceType();
        List<EObject> restaurants = List.copyOf(DynamicModels.getList(store, restaurantsReference));

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass simpleClass = DynamicModels.createEClass(viewType);
        EClass referencedClass = DynamicModels.createEClass(viewType);
        EReference reference = DynamicModels.createManyContainmentEReference(simpleClass, "containment", referencedClass);

        // View Setup
        EObject simple = DynamicModels.createEObject(simpleClass);
        ObjectBinding simpleBinding = createBinding(store, simple);

        // Operation Setup
        FeatureOriginOperation operation = new FeatureSource(restaurantsReference);

        // Pre-Action Get
        FeatureBinding result = operation.doGet(simpleBinding, context);

        // Pre-Action Change
        EObject inserted = DynamicModels.createEObject(referencedClass);
        EObject insertedRestaurant = DynamicModels.createEObject(restaurantClass);
        DynamicModels.getList(simple, reference).add(inserted);
        int index = DynamicModels.getList(simple, reference).indexOf(inserted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createInsertReferenceChange(simple, reference, inserted, index);

        // Action
        PutContext spyContext = spy(context);
        result = operation.doPut(change, result, simpleBinding, new ValueUpdateBinding.Insert(insertedRestaurant, index), spyContext);

        // Assertions
        verify(spyContext, times(1)).trackOriginObjectAttachmentChange(insertedRestaurant);
        assertEquals(new ValueBinding.Many(Streams.concat(Stream.of(insertedRestaurant), restaurants.stream()).toList()), result.value());
        assertEquals(Streams.concat(Stream.of(insertedRestaurant), restaurants.stream()).toList(), store.eGet(restaurantsReference));
    }

    @Test
    public void testPutOfRemoveShouldInsertRemoveOnOriginObject() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EReference restaurantsReference = DynamicModels.getEReference(store.eClass(), "restaurants");
        EClass restaurantClass = restaurantsReference.getEReferenceType();
        List<EObject> restaurants = List.copyOf(DynamicModels.getList(store, restaurantsReference));

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass simpleClass = DynamicModels.createEClass(viewType);
        EClass referencedClass = DynamicModels.createEClass(viewType);
        EReference reference = DynamicModels.createManyContainmentEReference(simpleClass, "containment", referencedClass);

        // View Setup
        EObject simple = DynamicModels.createEObject(simpleClass);
        ObjectBinding simpleBinding = createBinding(store, simple);

        // Operation Setup
        FeatureOriginOperation operation = new FeatureSource(restaurantsReference);

        // Pre-Action Get
        FeatureBinding result = operation.doGet(simpleBinding, context);

        // Pre-Action Change
        EObject removed = DynamicModels.createEObject(restaurantClass);
        EObject removedRestaurant = restaurants.get(0);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveReferenceChange(simple, reference, removed, 0);

        // Action
        PutContext spyContext = spy(context);
        result = operation.doPut(change, result, simpleBinding, new ValueUpdateBinding.Remove(removedRestaurant, 0), spyContext);

        // Assertions
        verify(spyContext, times(1)).trackOriginObjectAttachmentChange(removedRestaurant);
        assertEquals(new ValueBinding.Many(restaurants.stream().filter(entry -> entry != removedRestaurant).toList()), result.value());
        assertEquals(restaurants.stream().filter(entry -> entry != removedRestaurant).toList(), store.eGet(restaurantsReference));
    }


    @Test
    public void testPutOfUnsetShouldUnsetFeatureOnOriginObject() {
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
        FeatureOriginOperation operation = new FeatureSource(numEmployees);

        // Pre-Action Get
        FeatureBinding result = operation.doGet(simpleBinding, context);

        // Pre-Action Change
        simple.eUnset(numberAttribute);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createUnsetFeatureChange(simple, numberAttribute);

        // Action
        result = operation.doPut(change, result, simpleBinding, new ValueUpdateBinding.Unset(), context);

        // Assertions
        assertEquals(new ValueBinding.Unset(), result.value());
        assertEquals(0, restaurant.eGet(numEmployees));
    }
}