package tools.vitruv.compmodelcons.generator.backend.description;

import java.util.List;
import tools.vitruv.compmodelcons.generator.tools.Metamodel;

public record ViewTypeDescription(
    String className,
    String name,
    List<Metamodel> originMetamodels,
    Metamodel viewtypeMetamodel,
    RootDescription root,
    List<DeclarationDescription> declarations
) {
}
