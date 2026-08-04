package tools.vitruv.compmodelcons.change;

import com.google.common.collect.Sets;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.compmodelcons.change.impl.InternalReactionsChangePropagationSpecificationWrappingStrategy;
import tools.vitruv.compmodelcons.change.impl.NullViewChangePropagationSpecificationImpl;
import tools.vitruv.compmodelcons.change.impl.RemoteChangePropagationSpecificationWrappingStrategy;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReactionsChangePropagationSpecification;

import java.util.*;
import java.util.function.BiFunction;

public class ViewChangePropagationSpecificationAdapterFactory {
    public static final ViewChangePropagationSpecificationAdapterFactory INSTANCE =
            new ViewChangePropagationSpecificationAdapterFactory();

    private final Set<String> ignoredNsUris = new HashSet<>();

    public ViewChangePropagationSpecificationAdapterFactory() {
    }

    public void ignoreMetamodel(String nsUri) {
        ignoredNsUris.add(nsUri);
    }

    private List<ChangePropagationSpecification> create(Optional<ChangePropagationViewTypeSpecification> sourceViewType, ChangePropagationSpecification specification, Optional<ChangePropagationViewTypeSpecification> targetViewType, BiFunction<Integer, Integer, ChangePropagationSpecification> producer) {
        List<MetamodelDescriptor> sourceMetamodels = getMetamodelDescriptors(sourceViewType,
                                                                             specification.getSourceMetamodelDescriptor()
        );
        List<MetamodelDescriptor> targetMetamodels = getMetamodelDescriptors(targetViewType,
                                                                             specification.getTargetMetamodelDescriptor()
        );

        List<ChangePropagationSpecification> result =
                new ArrayList<>(sourceMetamodels.size() * targetMetamodels.size());

        for (int sourceMetamodelIndex = 0;
             sourceMetamodelIndex < sourceMetamodels.size();
             sourceMetamodelIndex++
        ) {
            if (!Sets.intersection(ignoredNsUris,
                                   sourceMetamodels.get(sourceMetamodelIndex).getNsUris()
            ).isEmpty()) {
                continue;
            }
            for (int targetMetamodelIndex = 0;
                 targetMetamodelIndex < targetMetamodels.size();
                 targetMetamodelIndex++
            ) {
                if (!Sets.intersection(ignoredNsUris,
                                       targetMetamodels.get(targetMetamodelIndex).getNsUris()
                ).isEmpty()) {
                    continue;
                }
                result.add(producer.apply(sourceMetamodelIndex, targetMetamodelIndex));
            }
        }

        return result;
    }

    private List<MetamodelDescriptor> getMetamodelDescriptors(Optional<ChangePropagationViewTypeSpecification> viewTypeSpecification, MetamodelDescriptor inner) {
        return viewTypeSpecification.map(ChangePropagationViewTypeSpecification::getOriginMetamodelDescriptors)
                .orElse(List.of(inner));
    }

    public List<ChangePropagationSpecification> createInternal(Optional<ChangePropagationViewTypeSpecification> sourceViewType, AbstractReactionsChangePropagationSpecification specification, Optional<ChangePropagationViewTypeSpecification> targetViewType, ChangeDeterminationMode changeDeterminationMode) {
        return create(sourceViewType,
                      specification,
                      targetViewType,
                      (sourceMetamodelIndex, targetMetamodelIndex) -> new ViewChangePropagationSpecificationAdapter(
                              sourceViewType.orElse(new NullViewChangePropagationSpecificationImpl(
                                      specification.getSourceMetamodelDescriptor())),
                              sourceMetamodelIndex,
                              new InternalReactionsChangePropagationSpecificationWrappingStrategy(
                                      specification),
                              targetViewType.orElse(new NullViewChangePropagationSpecificationImpl(
                                      specification.getTargetMetamodelDescriptor())),
                              targetMetamodelIndex,
                              changeDeterminationMode
                      )
        );
    }

    public List<ChangePropagationSpecification> createRemote(Optional<ChangePropagationViewTypeSpecification> sourceViewType, ChangePropagationSpecification specification, Optional<ChangePropagationViewTypeSpecification> targetViewType, ChangeDeterminationMode changeDeterminationMode) {
        return create(sourceViewType,
                      specification,
                      targetViewType,
                      (sourceMetamodelIndex, targetMetamodelIndex) -> new ViewChangePropagationSpecificationAdapter(
                              sourceViewType.orElse(new NullViewChangePropagationSpecificationImpl(
                                      specification.getSourceMetamodelDescriptor())),
                              sourceMetamodelIndex,
                              new RemoteChangePropagationSpecificationWrappingStrategy(specification),
                              targetViewType.orElse(new NullViewChangePropagationSpecificationImpl(
                                      specification.getTargetMetamodelDescriptor())),
                              targetMetamodelIndex,
                              changeDeterminationMode
                      )
        );
    }
}
