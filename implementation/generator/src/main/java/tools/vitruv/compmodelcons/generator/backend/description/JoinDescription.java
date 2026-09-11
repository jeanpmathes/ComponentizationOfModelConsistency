package tools.vitruv.compmodelcons.generator.backend.description;

import java.util.List;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenFeature;
import tools.vitruv.compmodelcons.views.operations.Join;

public record JoinDescription(
    GenClass sourceClass,
    SourceObjectFactoryDeclarationDescription sourceObjectFactoryDeclaration,
    OriginDescription origin,
    Join.Type type,
    List<FeatureCondition> featureConditions,
    List<ExpressionDescription> expressionConditions
) implements OriginDescription {
  public record FeatureCondition(
      int leftIndex,
      GenFeature leftFeature,
      int rightIndex,
      GenFeature rightFeature
  ) {
  }
}
