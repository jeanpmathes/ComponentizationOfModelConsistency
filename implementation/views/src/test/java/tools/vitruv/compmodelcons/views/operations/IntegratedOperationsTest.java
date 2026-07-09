package tools.vitruv.compmodelcons.views.operations;

import com.google.common.collect.Sets;
import org.eclipse.emf.ecore.*;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class IntegratedOperationsTest extends AbstractOperationTest {
    @Test
    public void testSimpleEmptyViewOfRestaurantsAsRootShouldSupportBothRemovingAndInsertingRestaurants() {
        // Origin Setup
        EObject store = models.getRoot(Model.RESTAURANT);
        EClass restaurantClass = DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);
        List<EObject> storeContents = List.copyOf(store.eContents());

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass emptyClass = DynamicModels.createEClass(viewType);

        // Operation Setup
        Root operation = new Root(emptyClass, Optional.of(new Project(emptyClass, new Source(restaurantClass), List.of())), List.of());

        // Action: Get the view.
        List<ObjectBinding> results = new ArrayList<>(operation.GET(context));

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

    @Test
    public void testSimpleViewOfRestaurantsShouldSupportSettingTheNumberOfEmployees() {
        // Origin Setup
        EClass restaurantClass = DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
        EStructuralFeature numEmployees = restaurantClass.getEStructuralFeature("numEmployees");

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType, "Root");
        EClass simpleClass = DynamicModels.createEClass(viewType, "Simple");
        EReference simpleContainment = DynamicModels.createContainmentEReference(rootClass, "simples", simpleClass);
        EAttribute employeeCount = DynamicModels.createEAttribute(simpleClass, "employeeCount", EcorePackage.eINSTANCE.getEInt());

        // Operation Setup
        Root operation = new Root(rootClass, Optional.empty(), List.of(new Root.Contained(simpleContainment, new Project(simpleClass, new Source(restaurantClass), List.of(new FeatureProject(employeeCount, new FeatureSource(numEmployees)))))));

        // Action: Get the view.
        List<ObjectBinding> results = new ArrayList<>(operation.GET(context));

        // Action: Unset the number of employees.
        for (EObject simple : DynamicModels.getList(context.getViewModel().getContents().get(0), simpleContainment)) {
            doUnset(simple, employeeCount, operation, results);
        }

        // Action: Set a new number of employees.
        for (EObject simple : DynamicModels.getList(context.getViewModel().getContents().get(0), simpleContainment)) {
            doReplaceAttribute(simple, employeeCount, 52, operation, results);
        }

        // Assertions
        assertForAll(context.getOriginObjects(restaurantClass), restaurant -> restaurant.eGet(numEmployees).equals(52));
    }

    private EObject doCreate(EClass eClass, Operation operation, List<ObjectBinding> roots) {
        EObject created = DynamicModels.createEObject(eClass);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);
        ObjectBinding result = operation.PUT(change, roots.get(0), context);
        roots.set(0, result);
        return created;
    }

    private void doDelete(EObject removed, Operation operation, List<ObjectBinding> roots) {
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(removed);
        ObjectBinding result = operation.PUT(change, roots.get(0), context);
        roots.set(0, result);
    }

    private void doInsertRoot(EObject inserted, Operation operation, List<ObjectBinding> roots) {
        context.getViewModel().getContents().add(inserted);
        int index = context.getViewModel().getContents().indexOf(inserted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createInsertRootChange(inserted, context.getViewModel(), index);
        ObjectBinding result = operation.PUT(change, roots.get(0), context);
        roots.set(0, result);
    }

    private void doRemoveRoot(EObject removed, Operation operation, List<ObjectBinding> roots) {
        int index = context.getViewModel().getContents().indexOf(removed);
        context.getViewModel().getContents().remove(removed);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveRootChange(removed, context.getViewModel(), index);
        ObjectBinding result = operation.PUT(change, roots.get(0), context);
        roots.set(0, result);
    }

    private void doUnset(EObject target, EStructuralFeature feature, Operation operation, List<ObjectBinding> roots) {
        target.eUnset(feature);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createUnsetFeatureChange(target, feature);
        ObjectBinding result = operation.PUT(change, roots.get(0), context);
        roots.set(0, result);
    }

    private void doReplaceAttribute(EObject target, EAttribute attribute, Object newValue, Operation operation, List<ObjectBinding> roots) {
        Object oldValue = target.eGet(attribute);
        target.eSet(attribute, newValue);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createReplaceSingleAttributeChange(target, attribute, oldValue, newValue);
        ObjectBinding result = operation.PUT(change, roots.get(0), context);
        roots.set(0, result);
    }
}
