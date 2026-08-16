package tools.vitruv.compmodelcons.views.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.bindings.OriginBinding;
import tools.vitruv.compmodelcons.views.conditions.Condition;

class FilterTest extends AbstractOperationTest {
  @Test
  public void testGetShouldReturnOnlyBindingsThatPassCondition() {
    // Origin Setup
    EClass restaurantClass =
        DynamicModels.getEClass(models.getPackage(Model.RESTAURANT), "Restaurant");
    List<EObject> restaurants = context.getOriginObjects(restaurantClass);
    OriginBinding first = OriginBinding.of(restaurants.getFirst());
    OriginBinding second = OriginBinding.of(restaurants.get(1));

    // Operation Setup
    OriginOperation originOperation = mock(OriginOperation.class);
    Condition condition = binding -> binding
        .originObjects()
        .contains(restaurants.getFirst());
    Filter operation = new Filter(condition, originOperation);

    // Action
    when(originOperation.doGet(context)).thenReturn(List.of(first, second));
    List<OriginBinding> result = operation.doGet(context);

    // Assertions
    verify(originOperation, times(1)).doGet(context);
    assertEquals(List.of(first), result);
  }

  @Test
  public void testPutShouldForwardCallToInnerOperation() {
    // Origin Setup
    EObject store = models.getRoot(Model.RESTAURANT);

    // ViewType Setup
    EPackage viewType = DynamicModels.createEPackage();
    EClass emptyClass = DynamicModels.createEClass(viewType);
    EObject created = DynamicModels.createEObject(emptyClass);
    EChange<EObject> change = TypeInferringAtomicEChangeFactory
        .getInstance()
        .createCreateEObjectChange(created);

    // Operation Setup
    OriginOperation originOperation = mock(OriginOperation.class);
    Filter operation = new Filter(binding -> true, originOperation);
    OriginBinding target = OriginBinding.empty();
    OriginBinding expected = OriginBinding.of(store);

    // Action
    when(originOperation.doPut(change, target, context)).thenReturn(expected);
    OriginBinding result = operation.doPut(change, target, context);

    // Assertions
    verify(originOperation, times(1)).doPut(change, target, context);
    assertEquals(expected, result);
  }
}