package tools.vitruv.compmodelcons.change;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReactionsChangePropagationSpecification;

class ViewBasedChangePropagationSpecificationAdapterFactoryTest {

  final MetamodelDescriptor a = MetamodelDescriptor.with("a");
  final MetamodelDescriptor b = MetamodelDescriptor.with("b");
  final MetamodelDescriptor ab = MetamodelDescriptor.with(Set.of("a", "b"));
  final MetamodelDescriptor c = MetamodelDescriptor.with("c");
  final MetamodelDescriptor d = MetamodelDescriptor.with("d");
  final MetamodelDescriptor cd = MetamodelDescriptor.with(Set.of("c", "d"));
  final MetamodelDescriptor x = MetamodelDescriptor.with("x");
  final MetamodelDescriptor y = MetamodelDescriptor.with("y");

  @Test
  void testCreateWithEmptyOptionals() {
    AbstractReactionsChangePropagationSpecification specification =
        mock(AbstractReactionsChangePropagationSpecification.class);
    when(specification.getSourceMetamodelDescriptor()).thenReturn(a);
    when(specification.getTargetMetamodelDescriptor()).thenReturn(b);

    ChangePropagationSpecification result =
        ViewChangePropagationSpecificationAdapterFactory.INSTANCE.create(Optional.empty(),
                                                                         specification,
                                                                         Optional.empty());

    assertEquals(a, result.getSourceMetamodelDescriptor());
    assertEquals(b, result.getTargetMetamodelDescriptor());
  }

  @Test
  void testCreateWithMultipleMetamodels() {
    ChangePropagatingViewTypeSpecification sourceViewType =
        mock(ChangePropagatingViewTypeSpecification.class);
    ChangePropagatingViewTypeSpecification targetViewType =
        mock(ChangePropagatingViewTypeSpecification.class);

    when(sourceViewType.getViewTypeMetamodelDescriptor()).thenReturn(x);
    when(targetViewType.getViewTypeMetamodelDescriptor()).thenReturn(y);
    when(sourceViewType.getOriginMetamodelDescriptor()).thenReturn(ab);
    when(targetViewType.getOriginMetamodelDescriptor()).thenReturn(cd);

    AbstractReactionsChangePropagationSpecification specification =
        mock(AbstractReactionsChangePropagationSpecification.class);
    when(specification.getSourceMetamodelDescriptor()).thenReturn(x);
    when(specification.getTargetMetamodelDescriptor()).thenReturn(y);

    ChangePropagationSpecification result =
        ViewChangePropagationSpecificationAdapterFactory.INSTANCE.create(
            Optional.of(sourceViewType), specification, Optional.of(targetViewType));

    assertEquals(ab, result.getSourceMetamodelDescriptor());
    assertEquals(cd, result.getTargetMetamodelDescriptor());
  }
}