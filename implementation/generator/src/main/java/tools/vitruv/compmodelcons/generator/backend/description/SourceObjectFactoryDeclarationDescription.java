package tools.vitruv.compmodelcons.generator.backend.description;

import org.eclipse.emf.codegen.ecore.genmodel.GenClass;

public record SourceObjectFactoryDeclarationDescription(
    GenClass sourceClass,
    GenClass targetClass,
    int index
) implements DeclarationDescription {
  public String getMethodName() {
    return "create" + sourceClass.getInterfaceName() + "For" + targetClass.getInterfaceName()
               + index;
  }
}
