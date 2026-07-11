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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals(42, simple.eGet(numberAttribute));
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
}