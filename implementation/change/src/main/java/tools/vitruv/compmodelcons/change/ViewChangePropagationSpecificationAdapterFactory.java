package tools.vitruv.compmodelcons.change;

import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.compmodelcons.change.impl.InternalReactionsChangePropagationSpecificationWrappingStrategy;
import tools.vitruv.compmodelcons.change.impl.NullViewChangePropagationSpecificationImpl;
import tools.vitruv.compmodelcons.change.impl.RemoteChangePropagationSpecificationWrappingStrategy;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReactionsChangePropagationSpecification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class ViewChangePropagationSpecificationAdapterFactory {
    public static final ViewChangePropagationSpecificationAdapterFactory INSTANCE = new ViewChangePropagationSpecificationAdapterFactory();

    private ViewChangePropagationSpecificationAdapterFactory() {
    }

    private List<ChangePropagationSpecification> create(Optional<ChangePropagationViewTypeSpecification> sourceViewType, Optional<ChangePropagationViewTypeSpecification> targetViewType, BiFunction<Integer, Integer, ChangePropagationSpecification> producer) {
        int sourceMetamodels = sourceViewType.map(v -> v.getOriginMetamodelDescriptors().size()).orElse(1);
        int targetMetamodels = targetViewType.map(v -> v.getOriginMetamodelDescriptors().size()).orElse(1);

        List<ChangePropagationSpecification> result = new ArrayList<>(sourceMetamodels * targetMetamodels);

        for (int sourceMetamodelIndex = 0; sourceMetamodelIndex < sourceMetamodels; sourceMetamodelIndex++) {
            for (int targetMetamodelIndex = 0; targetMetamodelIndex < targetMetamodels; targetMetamodelIndex++) {
                result.add(producer.apply(sourceMetamodelIndex, targetMetamodelIndex));
            }
        }

        return result;
    }

    public List<ChangePropagationSpecification> createInternal(Optional<ChangePropagationViewTypeSpecification> sourceViewType, AbstractReactionsChangePropagationSpecification specification, Optional<ChangePropagationViewTypeSpecification> targetViewType, ChangeDeterminationMode changeDeterminationMode) {
        return create(sourceViewType, targetViewType, (sourceMetamodelIndex, targetMetamodelIndex) -> new ViewChangePropagationSpecificationAdapter(
                sourceViewType.orElse(new NullViewChangePropagationSpecificationImpl(specification.getSourceMetamodelDescriptor())), sourceMetamodelIndex,
                new InternalReactionsChangePropagationSpecificationWrappingStrategy(specification),
                targetViewType.orElse(new NullViewChangePropagationSpecificationImpl(specification.getTargetMetamodelDescriptor())), targetMetamodelIndex,
                changeDeterminationMode));
    }

    public List<ChangePropagationSpecification> createRemote(Optional<ChangePropagationViewTypeSpecification> sourceViewType, ChangePropagationSpecification specification, Optional<ChangePropagationViewTypeSpecification> targetViewType, ChangeDeterminationMode changeDeterminationMode) {
        return create(sourceViewType, targetViewType, (sourceMetamodelIndex, targetMetamodelIndex) -> new ViewChangePropagationSpecificationAdapter(
                sourceViewType.orElse(new NullViewChangePropagationSpecificationImpl(specification.getSourceMetamodelDescriptor())), sourceMetamodelIndex,
                new RemoteChangePropagationSpecificationWrappingStrategy(specification),
                targetViewType.orElse(new NullViewChangePropagationSpecificationImpl(specification.getTargetMetamodelDescriptor())), targetMetamodelIndex,
                changeDeterminationMode));
    }
}
