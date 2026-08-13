package tools.vitruv.compmodelcons.change;

import org.junit.jupiter.api.Test;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReactionsChangePropagationSpecification;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void testCreateInternalWithEmptyOptionals() {
        AbstractReactionsChangePropagationSpecification specification = mock(AbstractReactionsChangePropagationSpecification.class);
        when(specification.getSourceMetamodelDescriptor()).thenReturn(a);
        when(specification.getTargetMetamodelDescriptor()).thenReturn(b);

        ChangePropagationSpecification result = ViewChangePropagationSpecificationAdapterFactory.INSTANCE.createInternal(Optional.empty(), specification, Optional.empty(), ChangeDeterminationMode.CHANGE_DERIVATION);

        assertEquals(a, result.getSourceMetamodelDescriptor());
        assertEquals(b, result.getTargetMetamodelDescriptor());
    }

    @Test
    void testCreateInternalWithMultipleMetamodels() {
        ChangePropagatingViewTypeSpecification sourceViewType = mock(ChangePropagatingViewTypeSpecification.class);
        ChangePropagatingViewTypeSpecification targetViewType = mock(ChangePropagatingViewTypeSpecification.class);

        when(sourceViewType.getViewTypeMetamodelDescriptor()).thenReturn(x);
        when(targetViewType.getViewTypeMetamodelDescriptor()).thenReturn(y);
        when(sourceViewType.getOriginMetamodelDescriptor()).thenReturn(ab);
        when(targetViewType.getOriginMetamodelDescriptor()).thenReturn(cd);

        AbstractReactionsChangePropagationSpecification specification = mock(AbstractReactionsChangePropagationSpecification.class);
        when(specification.getSourceMetamodelDescriptor()).thenReturn(x);
        when(specification.getTargetMetamodelDescriptor()).thenReturn(y);

        ChangePropagationSpecification result = ViewChangePropagationSpecificationAdapterFactory.INSTANCE.createInternal(
                Optional.of(sourceViewType), specification, Optional.of(targetViewType), ChangeDeterminationMode.CHANGE_DERIVATION);

        assertEquals(ab, result.getSourceMetamodelDescriptor());
        assertEquals(cd, result.getTargetMetamodelDescriptor());
    }

    @Test
    void testCreateRemoteWithEmptyOptionals() {
        AbstractReactionsChangePropagationSpecification specification = mock(AbstractReactionsChangePropagationSpecification.class);
        when(specification.getSourceMetamodelDescriptor()).thenReturn(a);
        when(specification.getTargetMetamodelDescriptor()).thenReturn(b);

        ChangePropagationSpecification result = ViewChangePropagationSpecificationAdapterFactory.INSTANCE.createRemote(Optional.empty(), specification, Optional.empty(), ChangeDeterminationMode.CHANGE_DERIVATION);

        assertEquals(a, result.getSourceMetamodelDescriptor());
        assertEquals(b, result.getTargetMetamodelDescriptor());
    }

    @Test
    void testCreateRemoteWithMultipleMetamodels() {
        ChangePropagatingViewTypeSpecification sourceViewType = mock(ChangePropagatingViewTypeSpecification.class);
        ChangePropagatingViewTypeSpecification targetViewType = mock(ChangePropagatingViewTypeSpecification.class);

        when(sourceViewType.getViewTypeMetamodelDescriptor()).thenReturn(x);
        when(targetViewType.getViewTypeMetamodelDescriptor()).thenReturn(y);
        when(sourceViewType.getOriginMetamodelDescriptor()).thenReturn(ab);
        when(targetViewType.getOriginMetamodelDescriptor()).thenReturn(cd);

        AbstractReactionsChangePropagationSpecification specification = mock(AbstractReactionsChangePropagationSpecification.class);
        when(specification.getSourceMetamodelDescriptor()).thenReturn(x);
        when(specification.getTargetMetamodelDescriptor()).thenReturn(y);

        ChangePropagationSpecification result = ViewChangePropagationSpecificationAdapterFactory.INSTANCE.createRemote(
                Optional.of(sourceViewType), specification, Optional.of(targetViewType), ChangeDeterminationMode.CHANGE_DERIVATION);

        assertEquals(ab, result.getSourceMetamodelDescriptor());
        assertEquals(cd, result.getTargetMetamodelDescriptor());
    }
}