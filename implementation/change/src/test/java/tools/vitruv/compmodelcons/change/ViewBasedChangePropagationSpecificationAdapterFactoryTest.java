package tools.vitruv.compmodelcons.change;

import org.junit.jupiter.api.Test;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReactionsChangePropagationSpecification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ViewBasedChangePropagationSpecificationAdapterFactoryTest {

    @Test
    void testCreateInternalWithEmptyOptionals() {
        AbstractReactionsChangePropagationSpecification specification = mock(AbstractReactionsChangePropagationSpecification.class);
        MetamodelDescriptor sourceMetamodel = mock(MetamodelDescriptor.class);
        MetamodelDescriptor targetMetamodel = mock(MetamodelDescriptor.class);
        when(specification.getSourceMetamodelDescriptor()).thenReturn(sourceMetamodel);
        when(specification.getTargetMetamodelDescriptor()).thenReturn(targetMetamodel);

        List<ChangePropagationSpecification> result = ViewChangePropagationSpecificationAdapterFactory.INSTANCE.createInternal(
                Optional.empty(), specification, Optional.empty(), ChangeDeterminationMode.CHANGE_DERIVATION);

        assertEquals(1, result.size());
    }

    @Test
    void testCreateInternalWithMultipleMetamodels() {
        ChangePropagatingViewTypeSpecification sourceViewType = mock(ChangePropagatingViewTypeSpecification.class);
        ChangePropagatingViewTypeSpecification targetViewType = mock(ChangePropagatingViewTypeSpecification.class);

        MetamodelDescriptor sourceMetamodel = mock(MetamodelDescriptor.class);
        MetamodelDescriptor targetMetamodel = mock(MetamodelDescriptor.class);
        when(sourceViewType.getViewTypeMetamodelDescriptor()).thenReturn(sourceMetamodel);
        when(targetViewType.getViewTypeMetamodelDescriptor()).thenReturn(targetMetamodel);

        when(sourceViewType.getOriginMetamodelDescriptors()).thenReturn(List.of(mock(MetamodelDescriptor.class), mock(MetamodelDescriptor.class)));
        when(targetViewType.getOriginMetamodelDescriptors()).thenReturn(List.of(mock(MetamodelDescriptor.class), mock(MetamodelDescriptor.class), mock(MetamodelDescriptor.class)));

        AbstractReactionsChangePropagationSpecification specification = mock(AbstractReactionsChangePropagationSpecification.class);
        when(specification.getSourceMetamodelDescriptor()).thenReturn(sourceMetamodel);
        when(specification.getTargetMetamodelDescriptor()).thenReturn(targetMetamodel);

        List<ChangePropagationSpecification> result = ViewChangePropagationSpecificationAdapterFactory.INSTANCE.createInternal(
                Optional.of(sourceViewType), specification, Optional.of(targetViewType), ChangeDeterminationMode.CHANGE_DERIVATION);

        assertEquals(2 * 3, result.size());
    }

    @Test
    void testCreateRemoteWithEmptyOptionals() {
        ChangePropagationSpecification specification = mock(ChangePropagationSpecification.class);
        MetamodelDescriptor sourceMetamodel = mock(MetamodelDescriptor.class);
        MetamodelDescriptor targetMetamodel = mock(MetamodelDescriptor.class);
        when(specification.getSourceMetamodelDescriptor()).thenReturn(sourceMetamodel);
        when(specification.getTargetMetamodelDescriptor()).thenReturn(targetMetamodel);

        List<ChangePropagationSpecification> result = ViewChangePropagationSpecificationAdapterFactory.INSTANCE.createRemote(
                Optional.empty(), specification, Optional.empty(), ChangeDeterminationMode.UPDATING_GET);

        assertEquals(1, result.size());
    }

    @Test
    void testCreateRemoteWithMultipleMetamodels() {
        ChangePropagatingViewTypeSpecification sourceViewType = mock(ChangePropagatingViewTypeSpecification.class);
        ChangePropagatingViewTypeSpecification targetViewType = mock(ChangePropagatingViewTypeSpecification.class);

        MetamodelDescriptor sourceMetamodel = mock(MetamodelDescriptor.class);
        MetamodelDescriptor targetMetamodel = mock(MetamodelDescriptor.class);
        when(sourceViewType.getViewTypeMetamodelDescriptor()).thenReturn(sourceMetamodel);
        when(targetViewType.getViewTypeMetamodelDescriptor()).thenReturn(targetMetamodel);

        when(sourceViewType.getOriginMetamodelDescriptors()).thenReturn(List.of(mock(MetamodelDescriptor.class)));
        when(targetViewType.getOriginMetamodelDescriptors()).thenReturn(List.of(mock(MetamodelDescriptor.class), mock(MetamodelDescriptor.class)));

        ChangePropagationSpecification specification = mock(ChangePropagationSpecification.class);
        when(specification.getSourceMetamodelDescriptor()).thenReturn(sourceMetamodel);
        when(specification.getTargetMetamodelDescriptor()).thenReturn(targetMetamodel);

        List<ChangePropagationSpecification> result = ViewChangePropagationSpecificationAdapterFactory.INSTANCE.createRemote(
                Optional.of(sourceViewType), specification, Optional.of(targetViewType), ChangeDeterminationMode.UPDATING_GET);

        assertEquals(2, result.size());
    }
}