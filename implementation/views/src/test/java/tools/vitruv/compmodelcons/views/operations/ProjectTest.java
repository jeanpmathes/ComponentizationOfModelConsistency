package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.*;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProjectTest extends AbstractOperationTest {
    @Test
    public void testGetShouldCreateCorrespondence() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        Operation originOperation = mock(Operation.class);
        Project operation = new Project(emptyClass, originOperation, List.of());

        // Action
        when(originOperation.GET(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        List<ObjectBinding> result = operation.GET(context);

        // Assertions
        verify(originOperation, times(1)).GET(context);
        assertEquals(1, result.size());
        assertEquals(result.get(0).viewObject().eClass(), emptyClass);
        assertTrue(correspondences.correspond(List.of(store), result.get(0).viewObject()));
        assertFalse(context.getViewModel().getContents().contains(result.get(0).viewObject()));
    }

    @Test
    public void testGetShouldCallGetOfFeaturesWithCreatedViewObject() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass simpleClass = DynamicModels.createEClass(viewType);
        EAttribute number = DynamicModels.createEAttribute(simpleClass, "number", EcorePackage.eINSTANCE.getEInt());

        // Operation Setup
        Operation originOperation = mock(Operation.class);
        FeatureProject featureProject = mock(FeatureProject.class);
        when(featureProject.getCreatedFeature()).thenReturn(number);
        Project operation = new Project(simpleClass, originOperation, List.of(featureProject));

        // Action
        when(originOperation.GET(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        when(featureProject.GET(any(), any())).then(invocation -> {
            ObjectBinding binding = invocation.getArgument(0);
            binding.viewObject().eSet(number, 42);
            return createBinding(binding.originObjects().get(0), binding.viewObject(), 42);
        });
        List<ObjectBinding> result = operation.GET(context);

        // Assertions
        assertEquals(1, result.size());
        verify(featureProject, times(1)).GET(result.get(0), context);
        assertEquals(42, result.get(0).viewObject().eGet(number));
    }

    @Test
    public void testPutWithNoOriginObjectShouldCallTheInnerOperation() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EObject otherStore = DynamicModels.createEObject(store.eClass());

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        Operation originOperation = mock(Operation.class);
        Project operation = new Project(emptyClass, originOperation, List.of());

        // Pre-Action Get
        when(originOperation.GET(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        operation.GET(context);

        // Pre-Action Change
        EObject created = DynamicModels.createEObject(emptyClass);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);

        // Action
        when(originOperation.PUT(any(), any(), any())).thenReturn(ObjectBinding.ofOriginObject(otherStore));
        ObjectBinding result = operation.PUT(change, ObjectBinding.ofViewObject(created), context);

        // Assertions
        verify(originOperation, times(1)).PUT(eq(change), any(), eq(context));
        assertEquals(created, result.viewObject());
        assertEquals(otherStore, result.originObjects().get(0));
    }

    @Test
    public void testPutWithOriginObjectShouldCallTheInnerOperation() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        Operation originOperation = mock(Operation.class);
        Project operation = new Project(emptyClass, originOperation, List.of());

        // Pre-Action Get
        when(originOperation.GET(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        List<ObjectBinding> results = operation.GET(context);

        // Pre-Action Change
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(results.get(0).viewObject());

        // Action
        when(originOperation.PUT(any(), any(), any())).thenReturn(ObjectBinding.ofOriginObject(results.get(0).originObjects().get(0)));
        ObjectBinding result = operation.PUT(change, results.get(0), context);

        // Assertions
        verify(originOperation, times(1)).PUT(eq(change), any(), eq(context));
        assertEquals(results.get(0).viewObject(), result.viewObject());
        assertEquals(results.get(0).originObjects(), result.originObjects());
    }

    @Test
    public void testPutOfFeatureChangeShouldCallTheFeatureOperation() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass simpleClass = DynamicModels.createEClass(viewType);
        EAttribute number = DynamicModels.createEAttribute(simpleClass, "number", EcorePackage.eINSTANCE.getEInt());

        // Operation Setup
        Operation originOperation = mock(Operation.class);
        FeatureProject featureProject = mock(FeatureProject.class);
        when(featureProject.getCreatedFeature()).thenReturn(number);
        Project operation = new Project(simpleClass, originOperation, List.of(featureProject));

        // Pre-Action Get
        when(originOperation.GET(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        AtomicReference<FeatureBinding> createdFeatureBinding = new AtomicReference<>();
        when(featureProject.GET(any(), any())).then(invocation -> {
            ObjectBinding binding = invocation.getArgument(0);
            binding.viewObject().eSet(number, 67);
            createdFeatureBinding.set(createBinding(binding.originObjects().get(0), binding.viewObject(), 67));
            return createdFeatureBinding.get();
        });
        List<ObjectBinding> results = operation.GET(context);

        // Pre-Action Change
        results.get(0).viewObject().eUnset(number);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createUnsetFeatureChange(results.get(0).viewObject(), number);

        // Action
        when(featureProject.PUT(any(), any(), any(), any(), any())).thenReturn(createBinding(results.get(0).originObjects().get(0), results.get(0).viewObject(), 0));
        operation.PUT(change, results.get(0), context);

        // Assertions
        verify(originOperation, never()).PUT(any(), any(), any());
        //noinspection OptionalAssignedToNull
        verify(featureProject, times(1)).PUT(change, createdFeatureBinding.get(), results.get(0), null, context);
    }
}
