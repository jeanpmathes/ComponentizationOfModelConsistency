package tools.vitruv.compmodelcons.views.operations;

import com.google.common.collect.Sets;
import org.eclipse.emf.ecore.*;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.DynamicModels;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class IntegratedOperationsTest extends AbstractOperationTest {
    private Root.ViewBinding[] wrap(Root.ViewBinding view) {
        return new Root.ViewBinding[]{view};
    }

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
        var view = wrap(operation.doGet(context));

        // Action: Remove and delete one of the restaurants by removing and deleting an empty.
        EObject removed = models.getViewModel().getContents().getFirst();
        doRemoveRoot(removed, operation, view);
        doDelete(removed, operation, view);

        // Action: Create and insert a new restaurant by creating and inserting an empty.
        EObject created = doCreate(emptyClass, operation, view);
        doInsertRoot(created, operation, view);

        // Assertions
        assertTrue(models.getViewModel().getContents().contains(created));
        assertFalse(models.getViewModel().getContents().contains(removed));
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
        EReference simpleContainment = DynamicModels.createManyContainmentEReference(rootClass, "simples", simpleClass);
        EAttribute employeeCount = DynamicModels.createEAttribute(simpleClass, "employeeCount", EcorePackage.eINSTANCE.getEInt());

        // Operation Setup
        Root operation = new Root(rootClass, Optional.empty(), List.of(new Root.Target(simpleContainment, new Project(simpleClass, new Source(restaurantClass), List.of(new FeatureProject(Optional.of(numEmployees), employeeCount, new FeatureSource(numEmployees)))))));

        // Action: Get the view.
        var view = wrap(operation.doGet(context));

        // Action: Unset the number of employees.
        for (EObject simple : DynamicModels.getList(models.getViewModel().getContents().getFirst(), simpleContainment)) {
            doUnset(simple, employeeCount, operation, view);
        }

        // Action: Set a new number of employees.
        for (EObject simple : DynamicModels.getList(models.getViewModel().getContents().getFirst(), simpleContainment)) {
            doReplaceAttribute(simple, employeeCount, 52, operation, view);
        }

        // Assertions
        assertForAll(context.getOriginObjects(restaurantClass), restaurant -> restaurant.eGet(numEmployees).equals(52));
    }

    @Test
    public void testTripleJoinedViewShouldCreateElementInEachSource() {
        // Origin Setup
        EClass restaurantClass = DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
        EClass foodClass = DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Food");
        EClass reviewClass = DynamicModels.getEClass(models.getPackage(Model.REVIEWPAGE), "Review");
        List<EObject> restaurants = context.getOriginObjects(restaurantClass);
        List<EObject> foods = context.getOriginObjects(foodClass);
        List<EObject> reviews = context.getOriginObjects(reviewClass);

        // ViewType Setup
        EPackage viewType = DynamicModels.createEPackage();
        EClass rootClass = DynamicModels.createEClass(viewType, "Root");
        EClass joinedClass = DynamicModels.createEClass(viewType, "Joined");
        EReference joinedContainment = DynamicModels.createManyContainmentEReference(rootClass, "allJoined", joinedClass);

        // Operation Setup
        Root operation = new Root(rootClass, Optional.empty(), List.of(new Root.Target(joinedContainment, new Project(joinedClass, new Join(reviewClass, new Join(foodClass, new Source(restaurantClass))), List.of()))));

        // Action: Get the view.
        var view = wrap(operation.doGet(context));

        // Action: Create a new joined element.
        doCreate(joinedClass, operation, view);

        // Assertions
        assertEquals(restaurants.size() + 1, context.getOriginObjects(restaurantClass).size());
        assertEquals(foods.size() + 1, context.getOriginObjects(foodClass).size());
        assertEquals(reviews.size() + 1, context.getOriginObjects(reviewClass).size());
    }

    private EObject doCreate(EClass eClass, Root root, Root.ViewBinding[] view) {
        EObject created = DynamicModels.createEObject(eClass);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createCreateEObjectChange(created);
        view[0] = root.doPut(change, view[0], context);
        return created;
    }

    private void doDelete(EObject removed, Root root, Root.ViewBinding[] view) {
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createDeleteEObjectChange(removed);
        view[0] = root.doPut(change, view[0], context);
    }

    private void doInsertRoot(EObject inserted, Root root, Root.ViewBinding[] view) {
        models.getViewModel().getContents().add(inserted);
        int index = models.getViewModel().getContents().indexOf(inserted);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createInsertRootChange(inserted, models.getViewModel(), index);
        view[0] = root.doPut(change, view[0], context);
    }

    private void doRemoveRoot(EObject removed, Root root, Root.ViewBinding[] view) {
        int index = models.getViewModel().getContents().indexOf(removed);
        models.getViewModel().getContents().remove(removed);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createRemoveRootChange(removed, models.getViewModel(), index);
        view[0] = root.doPut(change, view[0], context);
    }

    private void doUnset(EObject target, EStructuralFeature feature, Root root, Root.ViewBinding[] view) {
        target.eUnset(feature);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createUnsetFeatureChange(target, feature);
        view[0] = root.doPut(change, view[0], context);
    }

    private void doReplaceAttribute(EObject target, EAttribute attribute, Object newValue, Root root, Root.ViewBinding[] view) {
        Object oldValue = target.eGet(attribute);
        target.eSet(attribute, newValue);
        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createReplaceSingleAttributeChange(target, attribute, oldValue, newValue);
        view[0] = root.doPut(change, view[0], context);
    }
}
