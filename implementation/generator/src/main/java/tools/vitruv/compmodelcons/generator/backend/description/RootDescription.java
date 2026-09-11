package tools.vitruv.compmodelcons.generator.backend.description;

import java.util.List;
import java.util.Optional;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenFeature;

public record RootDescription(
    GenClass rootClass,
    Optional<ProjectDescription> rootProject,
    List<Contained> contained
) {
  public record Contained(GenFeature feature, ProjectDescription project) {

  }
}
