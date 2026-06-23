package tools.vitruv.compmodelcons.change;

import tools.vitruv.compmodelcons.change.impl.ChangePropagationSpecificationWrapperImpl;
import tools.vitruv.compmodelcons.change.impl.NullViewChangePropagationSpecificationImpl;
import tools.vitruv.compmodelcons.change.impl.InternalReactionsCorrespondenceHandlingStrategyImpl;
import tools.vitruv.compmodelcons.change.impl.RemoteCorrespondenceHandlingStrategyImpl;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReactionsChangePropagationSpecification;

import java.util.Optional;

public class ViewChangePropagationSpecificationAdapterFactory {
    public static final ViewChangePropagationSpecificationAdapterFactory INSTANCE = new ViewChangePropagationSpecificationAdapterFactory();

    private ViewChangePropagationSpecificationAdapterFactory() {
    }

    public ViewChangePropagationSpecificationAdapter createInternal(Optional<ViewChangePropagationSpecification> sourceViewType, AbstractReactionsChangePropagationSpecification specification, Optional<ViewChangePropagationSpecification> targetViewType) {
        return new ViewChangePropagationSpecificationAdapter(
                sourceViewType.orElse(new NullViewChangePropagationSpecificationImpl(specification.getSourceMetamodelDescriptor())),
                new ChangePropagationSpecificationWrapperImpl(specification, new InternalReactionsCorrespondenceHandlingStrategyImpl(specification)),
                targetViewType.orElse(new NullViewChangePropagationSpecificationImpl(specification.getTargetMetamodelDescriptor())));
    }

    public ViewChangePropagationSpecificationAdapter createRemote(Optional<ViewChangePropagationSpecification> sourceViewType, AbstractReactionsChangePropagationSpecification specification, Optional<ViewChangePropagationSpecification> targetViewType) {
        return new ViewChangePropagationSpecificationAdapter(
                sourceViewType.orElse(new NullViewChangePropagationSpecificationImpl(specification.getSourceMetamodelDescriptor())),
                new ChangePropagationSpecificationWrapperImpl(specification, new RemoteCorrespondenceHandlingStrategyImpl()),
                targetViewType.orElse(new NullViewChangePropagationSpecificationImpl(specification.getTargetMetamodelDescriptor())));
    }
}
