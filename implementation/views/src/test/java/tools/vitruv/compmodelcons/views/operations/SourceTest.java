package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SourceTest extends AbstractOperationTest {
    @Test
    public void testStoreGet() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass storeClass = store.eClass();
        List<EObject> scores = context.getOriginObjects(storeClass);

        // Operation Setup
        Source source = new Source(storeClass);

        // Action
        List<ObjectBinding> result = source.get(context);

        // Assertions
        assertEquals(scores.size(), result.size());
        assertForAll(result, binding -> binding.originObjects().size() == 1);
        assertForAll(result, binding -> scores.contains(binding.originObjects().get(0)));
    }
}
