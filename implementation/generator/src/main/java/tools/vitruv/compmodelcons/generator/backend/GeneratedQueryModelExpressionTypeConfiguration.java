package tools.vitruv.compmodelcons.generator.backend;

import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.jspecify.annotations.NonNull;
import tools.vitruv.compmodelcons.generator.tools.NamingGenerator;
import tools.vitruv.neojoin.QueryModelExpressionTypeConfiguration;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;

public class GeneratedQueryModelExpressionTypeConfiguration
    implements QueryModelExpressionTypeConfiguration {
  @Override
  public void configure(@NonNull JvmGenericType queryModelExpressionType,
                        @NonNull ViewTypeDefinition viewTypeDefinition) {
    String name = viewTypeDefinition
        .eResource()
        .getURI()
        .trimFileExtension()
        .lastSegment();

    queryModelExpressionType.setSimpleName(
        NamingGenerator.convertToPascalCase(name) + "ViewTypeExpressions");
    queryModelExpressionType.setPackageName(
        NamingGenerator.PACKAGE_BASE + "." + viewTypeDefinition
            .getExport()
            .getPackage());
    queryModelExpressionType.setVisibility(JvmVisibility.DEFAULT);
    queryModelExpressionType.setFinal(true);
  }
}
