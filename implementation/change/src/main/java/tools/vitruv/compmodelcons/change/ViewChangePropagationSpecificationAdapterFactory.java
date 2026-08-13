package tools.vitruv.compmodelcons.change;

import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.compmodelcons.change.impl.InternalReactionsChangePropagationSpecificationWrappingStrategy;
import tools.vitruv.compmodelcons.change.impl.NullViewChangePropagatingSpecificationImpl;
import tools.vitruv.compmodelcons.change.impl.RemoteChangePropagationSpecificationWrappingStrategy;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReactionsChangePropagationSpecification;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ViewChangePropagationSpecificationAdapterFactory {
    public static final ViewChangePropagationSpecificationAdapterFactory INSTANCE = new ViewChangePropagationSpecificationAdapterFactory();

    private final Set<String> ignoredNsUris = new HashSet<>();

    public ViewChangePropagationSpecificationAdapterFactory() {
    }

    public void ignoreMetamodel(String nsUri) {
        ignoredNsUris.add(nsUri);
    }

    private ChangePropagationSpecification create(Optional<ChangePropagatingViewTypeSpecification> sourceViewType, ChangePropagationSpecification specification, Optional<ChangePropagatingViewTypeSpecification> targetViewType, BiFunction<MetamodelDescriptor, MetamodelDescriptor, ChangePropagationSpecification> producer) {
        MetamodelDescriptor sourceMetamodel = sourceViewType
                .map(ChangePropagatingViewTypeSpecification::getOriginMetamodelDescriptor)
                .orElse(specification.getSourceMetamodelDescriptor());
        MetamodelDescriptor targetMetamodel = targetViewType
                .map(ChangePropagatingViewTypeSpecification::getOriginMetamodelDescriptor)
                .orElse(specification.getTargetMetamodelDescriptor());

        return producer.apply(cleanUpMetamodelDescriptor(sourceMetamodel), cleanUpMetamodelDescriptor(targetMetamodel));
    }

    private MetamodelDescriptor cleanUpMetamodelDescriptor(MetamodelDescriptor metamodelDescriptor) {
        return MetamodelDescriptor.with(metamodelDescriptor.getNsUris().stream()
                                                           .filter(nsUri -> !ignoredNsUris.contains(nsUri))
                                                           .collect(Collectors.toSet()));
    }

    public ChangePropagationSpecification createInternal(Optional<ChangePropagatingViewTypeSpecification> sourceViewType, AbstractReactionsChangePropagationSpecification specification, Optional<ChangePropagatingViewTypeSpecification> targetViewType, ChangeDeterminationMode changeDeterminationMode) {
        return create(sourceViewType, specification, targetViewType, (sourceMetamodel, targetMetamodel) -> new ViewBasedChangePropagationSpecificationAdapter(sourceViewType.orElse(new NullViewChangePropagatingSpecificationImpl(specification.getSourceMetamodelDescriptor())), sourceMetamodel, new InternalReactionsChangePropagationSpecificationWrappingStrategy(specification), targetViewType.orElse(new NullViewChangePropagatingSpecificationImpl(specification.getTargetMetamodelDescriptor())), targetMetamodel, changeDeterminationMode));
    }

    public ChangePropagationSpecification createRemote(Optional<ChangePropagatingViewTypeSpecification> sourceViewType, ChangePropagationSpecification specification, Optional<ChangePropagatingViewTypeSpecification> targetViewType, ChangeDeterminationMode changeDeterminationMode) {
        return create(sourceViewType, specification, targetViewType, (sourceMetamodel, targetMetamodel) -> new ViewBasedChangePropagationSpecificationAdapter(sourceViewType.orElse(new NullViewChangePropagatingSpecificationImpl(specification.getSourceMetamodelDescriptor())), sourceMetamodel, new RemoteChangePropagationSpecificationWrappingStrategy(specification), targetViewType.orElse(new NullViewChangePropagatingSpecificationImpl(specification.getTargetMetamodelDescriptor())), targetMetamodel, changeDeterminationMode));
    }
}
