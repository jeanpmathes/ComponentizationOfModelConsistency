package tools.vitruv.compmodelcons.change;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceTranslationStrategy;
import tools.vitruv.compmodelcons.change.correspondence.impl.ViewIdCorrespondenceTranslationStrategyImpl;
import tools.vitruv.compmodelcons.change.impl.NullViewChangePropagatingSpecificationImpl;

public class ViewChangePropagationSpecificationAdapterFactory {
  public static final ViewChangePropagationSpecificationAdapterFactory INSTANCE =
      new ViewChangePropagationSpecificationAdapterFactory();

  private final Set<String> ignoredNsUris = new HashSet<>();

  public ViewChangePropagationSpecificationAdapterFactory() {
  }

  public void ignoreMetamodel(String nsUri) {
    ignoredNsUris.add(nsUri);
  }

  private ChangePropagationSpecification create(
      Optional<ChangePropagatingViewTypeSpecification> sourceViewType,
      ChangePropagationSpecification specification,
      Optional<ChangePropagatingViewTypeSpecification> targetViewType,
      BiFunction<MetamodelDescriptor, MetamodelDescriptor, ChangePropagationSpecification> producer) {
    MetamodelDescriptor sourceMetamodel = sourceViewType
        .map(ChangePropagatingViewTypeSpecification::getOriginMetamodelDescriptor)
        .orElse(specification.getSourceMetamodelDescriptor());
    MetamodelDescriptor targetMetamodel = targetViewType
        .map(ChangePropagatingViewTypeSpecification::getOriginMetamodelDescriptor)
        .orElse(specification.getTargetMetamodelDescriptor());
    return producer.apply(cleanUpMetamodelDescriptor(sourceMetamodel),
                          cleanUpMetamodelDescriptor(targetMetamodel));
  }

  private ChangePropagationSpecification create(
      Optional<ChangePropagatingViewTypeSpecification> sourceViewType,
      ChangePropagationSpecification specification,
      Optional<ChangePropagatingViewTypeSpecification> targetViewType,
      CorrespondenceTranslationStrategy correspondenceTranslationStrategy) {
    return create(sourceViewType, specification, targetViewType,
                  (sourceMetamodel, targetMetamodel) -> new ViewBasedChangePropagationSpecificationAdapter(
                      sourceViewType.orElse(new NullViewChangePropagatingSpecificationImpl(
                          specification.getSourceMetamodelDescriptor())), sourceMetamodel,
                      specification, correspondenceTranslationStrategy, targetViewType.orElse(
                      new NullViewChangePropagatingSpecificationImpl(
                          specification.getTargetMetamodelDescriptor())), targetMetamodel));
  }

  private MetamodelDescriptor cleanUpMetamodelDescriptor(MetamodelDescriptor metamodelDescriptor) {
    return MetamodelDescriptor.with(metamodelDescriptor
                                        .getNsUris()
                                        .stream()
                                        .filter(nsUri -> !ignoredNsUris.contains(nsUri))
                                        .collect(Collectors.toSet()));
  }

  public ChangePropagationSpecification create(
      Optional<ChangePropagatingViewTypeSpecification> sourceViewType,
      ChangePropagationSpecification specification,
      Optional<ChangePropagatingViewTypeSpecification> targetViewType) {
    return create(sourceViewType, specification, targetViewType,
                  new ViewIdCorrespondenceTranslationStrategyImpl());
  }
}
