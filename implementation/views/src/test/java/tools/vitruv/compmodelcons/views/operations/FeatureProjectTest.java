package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.*;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

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
        FeatureProject operation = new FeatureProject(numberAttribute, originOperation);

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
        FeatureOperation operation = new FeatureProject(numberAttribute, originOperation);

        // Action
        when(originOperation.GET(simpleBinding, context)).thenReturn(FeatureBinding.ofOriginObject(store, 42));
        FeatureBinding result = operation.GET(simpleBinding, context);

        // Assertions
        verify(originOperation, times(1)).GET(simpleBinding, context);
        assertEquals(42, result.value());
        assertEquals(42, simple.eGet(numberAttribute));
        assertEquals(simple, result.viewSubjectObject());
        assertEquals(1, result.originSubjectObjects().size());
        assertEquals(store, result.originSubjectObjects().get(0));
    }

    @Test
    public void testPutOfUnsetShouldPassEmptyOptionalToInnerOperation() {
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
        FeatureOperation operation = new FeatureProject(numberAttribute, originOperation);

        // Pre-Action Get
        when(originOperation.GET(simpleBinding, context)).thenReturn(FeatureBinding.ofOriginObject(store, 42));
        FeatureBinding result = operation.GET(simpleBinding, context);

        // Pre-Action Change
        simple.eUnset(numberAttribute);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createUnsetFeatureChange(simple, numberAttribute);

        // Action
        when(originOperation.PUT(any(), any(), any(), any(), any())).thenReturn(FeatureBinding.ofOriginObject(store, 0));
        result = operation.PUT(change, result, simpleBinding, Optional.empty(), context);

        // Assertions
        verify(originOperation, times(1)).PUT(eq(change), any(), eq(simpleBinding), eq(Optional.empty()), eq(context));
        assertEquals(0, result.value());
    }

    @Test
    public void testPutOfSetShouldPassEmptyOptionalToInnerOperation() {
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
        FeatureOperation operation = new FeatureProject(numberAttribute, originOperation);

        // Pre-Action Get
        when(originOperation.GET(simpleBinding, context)).thenReturn(FeatureBinding.ofOriginObject(store, 42));
        FeatureBinding result = operation.GET(simpleBinding, context);

        // Pre-Action Change
        simple.eSet(numberAttribute, 67);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createReplaceSingleAttributeChange(simple, numberAttribute, 42, 67);

        // Action
        when(originOperation.PUT(any(), any(), any(), any(), any())).thenReturn(FeatureBinding.ofOriginObject(store, 0));
        result = operation.PUT(change, result, simpleBinding, Optional.empty(), context);

        // Assertions
        verify(originOperation, times(1)).PUT(eq(change), any(), eq(simpleBinding), eq(Optional.of(67)), eq(context));
        assertEquals(67, result.value());
    }
}