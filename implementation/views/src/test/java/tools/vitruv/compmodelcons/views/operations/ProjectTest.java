package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.*;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueBinding;

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
        OriginOperation originOperation = mock(OriginOperation.class);
        Project operation = new Project(emptyClass, originOperation, List.of());

        // Action
        when(originOperation.doGet(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        List<ObjectBinding> result = operation.beginGetByCreatingViewObjects(context);
        for (ObjectBinding binding : result) {
            operation.completeGetByCallingGetOnFeatures(binding, context);
        }

        // Assertions
        verify(originOperation, times(1)).doGet(context);
        assertEquals(1, result.size());
        assertEquals(result.getFirst().viewObject().eClass(), emptyClass);
        assertTrue(correspondences.correspond(List.of(store), result.getFirst().viewObject()));
        assertFalse(models.getViewModel().getContents().contains(result.getFirst().viewObject()));
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
        OriginOperation originOperation = mock(OriginOperation.class);
        FeatureProject featureProject = mock(FeatureProject.class);
        when(featureProject.getCreatedFeature()).thenReturn(number);
        Project operation = new Project(simpleClass, originOperation, List.of(featureProject));

        // Action
        when(originOperation.doGet(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        when(featureProject.doGet(any(), any())).then(invocation -> {
            ObjectBinding binding = invocation.getArgument(0);
            binding.viewObject().eSet(number, 42);
            return createBinding(binding.originObjects().getFirst(), binding.viewObject(), ValueBinding.of(42));
        });
        List<ObjectBinding> result = operation.beginGetByCreatingViewObjects(context);
        for (ObjectBinding binding : result) {
            operation.completeGetByCallingGetOnFeatures(binding, context);
        }

        // Assertions
        assertEquals(1, result.size());
        verify(featureProject, times(1)).doGet(result.getFirst(), context);
        assertEquals(42, result.getFirst().viewObject().eGet(number));
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
        OriginOperation originOperation = mock(OriginOperation.class);
        Project operation = new Project(emptyClass, originOperation, List.of());

        // Pre-Action Get
        when(originOperation.doGet(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        for (ObjectBinding binding : operation.beginGetByCreatingViewObjects(context)) {
            operation.completeGetByCallingGetOnFeatures(binding, context);
        }

        // Pre-Action Change
        EObject created = DynamicModels.createEObject(emptyClass);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);

        // Action
        when(originOperation.doPut(any(), any(), any())).thenReturn(ObjectBinding.ofOriginObject(otherStore));
        ObjectBinding result = operation.doPut(change, ObjectBinding.ofViewObject(created), context);

        // Assertions
        verify(originOperation, times(1)).doPut(eq(change), any(), eq(context));
        assertEquals(created, result.viewObject());
        assertEquals(otherStore, result.originObjects().getFirst());
    }

    @Test
    public void testPutWithOriginObjectShouldCallTheInnerOperation() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        OriginOperation originOperation = mock(OriginOperation.class);
        Project operation = new Project(emptyClass, originOperation, List.of());

        // Pre-Action Get
        when(originOperation.doGet(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        List<ObjectBinding> results = operation.beginGetByCreatingViewObjects(context);
        for (ObjectBinding binding : results) {
            operation.completeGetByCallingGetOnFeatures(binding, context);
        }

        // Pre-Action Change
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(results.getFirst().viewObject());

        // Action
        when(originOperation.doPut(any(), any(), any())).thenReturn(ObjectBinding.ofOriginObject(results.getFirst().originObjects().getFirst()));
        ObjectBinding result = operation.doPut(change, results.getFirst(), context);

        // Assertions
        verify(originOperation, times(1)).doPut(eq(change), any(), eq(context));
        assertEquals(results.getFirst().viewObject(), result.viewObject());
        assertEquals(results.getFirst().originObjects(), result.originObjects());
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
        OriginOperation originOperation = mock(OriginOperation.class);
        FeatureProject featureProject = mock(FeatureProject.class);
        when(featureProject.getCreatedFeature()).thenReturn(number);
        Project operation = new Project(simpleClass, originOperation, List.of(featureProject));

        // Pre-Action Get
        when(originOperation.doGet(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        AtomicReference<FeatureBinding> createdFeatureBinding = new AtomicReference<>();
        when(featureProject.doGet(any(), any())).then(invocation -> {
            ObjectBinding binding = invocation.getArgument(0);
            binding.viewObject().eSet(number, 67);
            createdFeatureBinding.set(createBinding(binding.originObjects().getFirst(), binding.viewObject(), ValueBinding.of(67)));
            return createdFeatureBinding.get();
        });
        List<ObjectBinding> results = operation.beginGetByCreatingViewObjects(context);
        for (ObjectBinding binding : results) {
            operation.completeGetByCallingGetOnFeatures(binding, context);
        }

        // Pre-Action Change
        results.getFirst().viewObject().eUnset(number);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createUnsetFeatureChange(results.getFirst().viewObject(), number);

        // Action
        when(featureProject.doPut(any(), any(), any(), any())).thenReturn(createBinding(results.getFirst().originObjects().getFirst(), results.getFirst().viewObject(), ValueBinding.of(0)));
        operation.doPut(change, results.getFirst(), context);

        // Assertions
        verify(originOperation, never()).doPut(any(), any(), any());
        verify(featureProject, times(1)).doPut(change, createdFeatureBinding.get(), results.getFirst(), context);
    }
}
