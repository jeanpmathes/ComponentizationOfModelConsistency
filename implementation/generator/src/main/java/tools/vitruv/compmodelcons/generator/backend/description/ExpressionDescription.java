package tools.vitruv.compmodelcons.generator.backend.description;

import java.util.List;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;

public record ExpressionDescription(
    String qualifiedMethodName,
    List<Parameter> parameters
) {
  public record Parameter(GenClass parameterClass, int index) {
  }
}
