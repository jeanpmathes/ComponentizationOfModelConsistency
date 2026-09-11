package tools.vitruv.compmodelcons.generator.backend.description;

import java.util.List;
import org.eclipse.emf.codegen.ecore.genmodel.GenFeature;

public record FeatureSourceDescription(
    int targetIndex,
    List<GenFeature> targetFeatures
) implements FeatureOriginDescription {
}
