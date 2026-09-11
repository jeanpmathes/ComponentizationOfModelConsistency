package tools.vitruv.compmodelcons.generator.backend.description;

import java.util.Optional;
import org.eclipse.emf.codegen.ecore.genmodel.GenFeature;

public record FeatureProjectDescription(
    Optional<Integer> targetIndex,
    GenFeature createdFeature,
    FeatureOriginDescription origin
) {
}
