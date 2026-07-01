package tools.vitruv.compmodelcons.views.operations;

import com.google.common.collect.Sets;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.ArrayList;
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
        List<EObject> storeContents = List.copyOf(store.eContents());

        // ViewType Setup
        EPackage viewType = createEPackage();
        EClass emptyClass = createEClass(viewType);

        // Operation Setup
        Root operation = new Root(emptyClass, Optional.of(new Project(emptyClass, new Source(restaurantClass))), List.of());

        // Action: Get the view.
        List<ObjectBinding> results = new ArrayList<>(operation.get(context));

        // Action: Remove and delete one of the restaurants by removing and deleting an empty.
        EObject removed = context.getViewModel().getContents().get(0);
        doRemoveRoot(removed, operation, results);
        doDelete(removed, operation, results);

        // Action: Create and insert a new restaurant by creating and inserting an empty.
        EObject created = doCreate(emptyClass, operation, results);
        doInsertRoot(created, operation, results);

        // Assertions
        assertTrue(context.getViewModel().getContents().contains(created));
        assertFalse(context.getViewModel().getContents().contains(removed));
        assertEquals(restaurants.size(), context.getOriginObjects(restaurantClass).size());
        assertEquals(1, Sets.difference(new HashSet<>(restaurants), new HashSet<>(context.getOriginObjects(restaurantClass))).size());
        assertEquals(1, Sets.difference(new HashSet<>(context.getOriginObjects(restaurantClass)), new HashSet<>(restaurants)).size());
        assertEquals(storeContents.size(), store.eContents().size());
        assertEquals(1, Sets.difference(new HashSet<>(storeContents), new HashSet<>(store.eContents())).size());
        assertEquals(1, Sets.difference(new HashSet<>(store.eContents()), new HashSet<>(storeContents)).size());
    }

    private EObject doCreate(EClass eClass, Operation operation, List<ObjectBinding> roots) {
        EObject created = createEObject(eClass);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);
        ObjectBinding result = operation.put(change, roots.get(0), context);
        roots.set(0, result);
        return created;
    }

    private void doDelete(EObject removed, Operation operation, List<ObjectBinding> roots) {
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(removed);
        ObjectBinding result = operation.put(change, roots.get(0), context);
        roots.set(0, result);
    }

    private void doInsertRoot(EObject inserted, Operation operation, List<ObjectBinding> roots) {
        context.getViewModel().getContents().add(inserted);
        int index = context.getViewModel().getContents().indexOf(inserted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createInsertRootChange(inserted, context.getViewModel(), index);
        ObjectBinding result = operation.put(change, roots.get(0), context);
        roots.set(0, result);
    }

    private void doRemoveRoot(EObject removed, Operation operation, List<ObjectBinding> roots) {
        int index = context.getViewModel().getContents().indexOf(removed);
        context.getViewModel().getContents().remove(removed);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveRootChange(removed, context.getViewModel(), index);
        ObjectBinding result = operation.put(change, roots.get(0), context);
        roots.set(0, result);
    }
}
