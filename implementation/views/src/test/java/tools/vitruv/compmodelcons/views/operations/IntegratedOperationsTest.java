package tools.vitruv.compmodelcons.views.operations;

import com.google.common.collect.Sets;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class IntegratedOperationsTest extends AbstractOperationTest {
    @Test
    public void testSimpleEmptyViewOfRestaurantsAsRoot() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass restaurantClass = getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Project operation = new Project(emptyClass, new Source(restaurantClass));

        // Action: Remove and delete one of the restaurants.
        List<ObjectBinding> resultsOfGet = operation.get(context);
        ObjectBinding removed = resultsOfGet.get(0);
        doRemoveRoot(removed, operation);
        doDelete(removed, operation);

        // Action: Create and insert a new restaurant.
        EObject created = createEObject(emptyClass);
        ObjectBinding inserted = doCreate(created, operation);
        doInsertRoot(inserted, operation);

        // Assertions
        List<ObjectBinding> result = operation.get(context);
        assertEquals(restaurants.size(), result.size());
        assertEquals(restaurants.size(), context.getOriginObjects(restaurantClass).size());
        assertEquals(1, Sets.difference(new HashSet<>(restaurants), new HashSet<>(context.getOriginObjects(restaurantClass))).size());
        assertEquals(1, Sets.difference(new HashSet<>(context.getOriginObjects(restaurantClass)), new HashSet<>(restaurants)).size());
        assertTrue(store.eContents().contains(inserted.originObjects().get(0)));
        assertFalse(store.eContents().contains(removed.originObjects().get(0)));
    }

    private ObjectBinding doCreate(EObject created, Operation operation) {
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);
        Optional<ObjectBinding> result = operation.put(change, ObjectBinding.ofViewObject(created), context);
        assertTrue(result.isPresent());
        return result.get();
    }

    private void doDelete(ObjectBinding removed, Operation operation) {
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(removed.viewObject());
        Optional<ObjectBinding> result = operation.put(change, removed, context);
        assertFalse(result.isPresent());
    }

    private ObjectBinding doInsertRoot(ObjectBinding inserted, Operation operation) {
        context.getViewModel().getContents().add(inserted.viewObject());
        int index = context.getViewModel().getContents().indexOf(inserted.viewObject());
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createInsertRootChange(inserted.viewObject(), context.getViewModel(), index);
        Optional<ObjectBinding> result = operation.put(change, inserted, context);
        assertTrue(result.isPresent());
        return result.get();
    }

    private ObjectBinding doRemoveRoot(ObjectBinding removed, Operation operation) {
        int index = context.getViewModel().getContents().indexOf(removed.viewObject());
        context.getViewModel().getContents().remove(removed.viewObject());
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveRootChange(removed.viewObject(), context.getViewModel(), index);
        Optional<ObjectBinding> result = operation.put(change, removed, context);
        assertTrue(result.isPresent());
        return result.get();
    }
}
