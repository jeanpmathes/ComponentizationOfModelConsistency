package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProjectTest extends AbstractOperationTest {
    @Test
    public void testStore2EmptyGet() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Operation source = mock(Operation.class);
        when(source.get(context)).thenReturn(List.of(ObjectBinding.ofOriginObject(store)));
        Project operation = new Project(emptyClass, true, source);

        // Action
        List<ObjectBinding> result = operation.get(context);

        // Assertions
        assertEquals(1, result.size());
        assertEquals(result.get(0).viewObject().eClass(), emptyClass);
        assertTrue(correspondences.correspond(List.of(store), List.of(result.get(0).viewObject())));
    }
}
