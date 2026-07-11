package tools.vitruv.compmodelcons.change;

import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.compmodelcons.change.impl.ChangePropagationSpecificationWrapperImpl;
import tools.vitruv.compmodelcons.change.impl.InternalReactionsCorrespondenceHandlingStrategyImpl;
import tools.vitruv.compmodelcons.change.impl.NullViewChangePropagationSpecificationImpl;
import tools.vitruv.compmodelcons.change.impl.RemoteCorrespondenceHandlingStrategyImpl;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReactionsChangePropagationSpecification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class ViewChangePropagationSpecificationAdapterFactory {
    public static final ViewChangePropagationSpecificationAdapterFactory INSTANCE = new ViewChangePropagationSpecificationAdapterFactory();

    private ViewChangePropagationSpecificationAdapterFactory() {
    }

    private List<ChangePropagationSpecification> create(Optional<ViewChangePropagationSpecification> sourceViewType, Optional<ViewChangePropagationSpecification> targetViewType, BiFunction<Integer, Integer, ChangePropagationSpecification> producer) {
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

    public List<ChangePropagationSpecification> createInternal(Optional<ViewChangePropagationSpecification> sourceViewType, AbstractReactionsChangePropagationSpecification specification, Optional<ViewChangePropagationSpecification> targetViewType) {
        return create(sourceViewType, targetViewType, (sourceMetamodelIndex, targetMetamodelIndex) -> new ViewChangePropagationSpecificationAdapter(
                sourceViewType.orElse(new NullViewChangePropagationSpecificationImpl(specification.getSourceMetamodelDescriptor())), sourceMetamodelIndex,
                new ChangePropagationSpecificationWrapperImpl(specification, new InternalReactionsCorrespondenceHandlingStrategyImpl(specification)),
                targetViewType.orElse(new NullViewChangePropagationSpecificationImpl(specification.getTargetMetamodelDescriptor())), targetMetamodelIndex));
    }

    public List<ChangePropagationSpecification> createRemote(Optional<ViewChangePropagationSpecification> sourceViewType, ChangePropagationSpecification specification, Optional<ViewChangePropagationSpecification> targetViewType) {
        return create(sourceViewType, targetViewType, (sourceMetamodelIndex, targetMetamodelIndex) -> new ViewChangePropagationSpecificationAdapter(
                sourceViewType.orElse(new NullViewChangePropagationSpecificationImpl(specification.getSourceMetamodelDescriptor())), sourceMetamodelIndex,
                new ChangePropagationSpecificationWrapperImpl(specification, new RemoteCorrespondenceHandlingStrategyImpl()),
                targetViewType.orElse(new NullViewChangePropagationSpecificationImpl(specification.getTargetMetamodelDescriptor())), targetMetamodelIndex));
    }
}
