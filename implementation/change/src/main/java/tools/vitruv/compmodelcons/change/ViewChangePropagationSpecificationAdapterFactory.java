package tools.vitruv.compmodelcons.change;

import tools.vitruv.compmodelcons.change.impl.NullViewChangePropagationParticipationSpecificationImpl;
import tools.vitruv.compmodelcons.change.impl.ReactionsChangePropagationSpecificationWrapperImpl;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReactionsChangePropagationSpecification;

import java.util.Optional;

public class ViewChangePropagationSpecificationAdapterFactory {
    public static final ViewChangePropagationSpecificationAdapterFactory INSTANCE = new ViewChangePropagationSpecificationAdapterFactory();

    private ViewChangePropagationSpecificationAdapterFactory() {
    }

    public ViewChangePropagationSpecificationAdapter create(Optional<ViewChangePropagationParticipationSpecification> sourceViewType, AbstractReactionsChangePropagationSpecification specification, Optional<ViewChangePropagationParticipationSpecification> targetViewType) {
        return new ViewChangePropagationSpecificationAdapter(sourceViewType.orElse(new NullViewChangePropagationParticipationSpecificationImpl(specification.getSourceMetamodelDescriptor())), new ReactionsChangePropagationSpecificationWrapperImpl(specification), targetViewType.orElse(new NullViewChangePropagationParticipationSpecificationImpl(specification.getTargetMetamodelDescriptor())));
    }
}
