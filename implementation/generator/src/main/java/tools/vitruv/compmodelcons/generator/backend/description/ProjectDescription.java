package tools.vitruv.compmodelcons.generator.backend.description;

import java.util.List;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;

public record ProjectDescription(
    GenClass targetClass,
    OriginDescription origin,
    List<FeatureProjectDescription> features,
    OnPutDeclarationDescription onPutDeclaration
) {
}
