package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.*;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueUpdateBinding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        FeatureOperation operation = new FeatureSource(numEmployees);

        // Action
        FeatureBinding result = operation.GET(simpleBinding, context);

        // Assertions
        assertEquals(new ValueBinding.Single(numEmployeesValue), result.value());
        assertThrows(UnsupportedOperationException.class, result::viewSubjectObject);
        assertEquals(1, result.originSubjectObjects().size());
        assertEquals(restaurant, result.originSubjectObjects().get(0));
    }

    @Test
    public void testPutOfSetShouldSetFeatureOnOriginObject() {
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
        FeatureOperation operation = new FeatureSource(numEmployees);

        // Pre-Action Get
        FeatureBinding result = operation.GET(simpleBinding, context);

        // Pre-Action Change
        simple.eSet(numberAttribute, numEmployeesValue + 3);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createReplaceSingleAttributeChange(simple, numberAttribute, numEmployeesValue, numEmployeesValue + 3);

        // Action
        result = operation.PUT(change, result, simpleBinding, new ValueUpdateBinding.Replace(numEmployeesValue + 3), context);

        // Assertions
        assertEquals(new ValueBinding.Single(numEmployeesValue + 3), result.value());
        assertEquals(numEmployeesValue + 3, restaurant.eGet(numEmployees));
    }

    @Test
    public void testPutOfUnsetShouldUnsetFeatureOnOriginObject() {
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
        FeatureOperation operation = new FeatureSource(numEmployees);

        // Pre-Action Get
        FeatureBinding result = operation.GET(simpleBinding, context);

        // Pre-Action Change
        simple.eUnset(numberAttribute);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createUnsetFeatureChange(simple, numberAttribute);

        // Action
        result = operation.PUT(change, result, simpleBinding, new ValueUpdateBinding.Unset(), context);

        // Assertions
        assertEquals(new ValueBinding.Unset(), result.value());
        assertEquals(0, restaurant.eGet(numEmployees));
    }
}