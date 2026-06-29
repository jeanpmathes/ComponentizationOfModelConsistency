package tools.vitruv.compmodelcons.generator.backend;

import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.compmodelcons.change.ChangeSpecificationAwareViewType;
import tools.vitruv.compmodelcons.generator.tools.NamingGenerator;
import tools.vitruv.dsls.common.JavaFileGenerator;
import tools.vitruv.dsls.common.JavaImportHelper;
import tools.vitruv.neojoin.aqr.AQR;

import java.util.Collection;
import java.util.List;

public class ViewTypeSourceGenerator {
    private final JavaImportHelper importHelper = new JavaImportHelper();

    private final String name;
    private final EPackage metamodel;
    private final AQR aqr;

    public ViewTypeSourceGenerator(String name, EPackage metamodel, AQR aqr) {
        this.name = NamingGenerator.convertToPascalCase(name);
        this.metamodel = metamodel;
        this.aqr = aqr;
    }

    public String generate() {
        return JavaFileGenerator.generateClass(getImplementation(), getPackageName(), importHelper);
    }

    private CharSequence getImplementation() {
        StringBuilder builder = new StringBuilder();

        importHelper.typeRef(ChangeSpecificationAwareViewType.class);

        builder.append("public class ").append(getClassName()).append(" extends ChangeSpecificationAwareViewType {\n");
        appendBody(builder);
        builder.append("}");

        return builder;
    }

    private void appendBody(StringBuilder builder) {
        builder.append("    private static final String name = \"").append(name).append("\";\n");

        importHelper.typeRef(EPackage.class);

        builder.append("    private static final EPackage metamodel = ").append("null").append(";\n"); // todo: fill

        builder.append("\n");

        builder.append("    public ").append(getClassName()).append("() {\n");
        builder.append("        super(name, metamodel);\n");
        builder.append("    }\n\n");

        importHelper.typeRef(MetamodelDescriptor.class);

        builder.append("    @Override\n");
        builder.append("    public MetamodelDescriptor getOriginMetamodelDescriptor() {\n");
        builder.append("        return MetamodelDescriptor.of((EPackage)null);\n"); // todo: fill
        builder.append("    }\n\n");

        builder.append("    @Override\n");
        builder.append("    public MetamodelDescriptor getViewTypeMetamodelDescriptor() {\n");
        builder.append("        return MetamodelDescriptor.of(metamodel);\n");
        builder.append("    }\n\n");

        importHelper.typeRef(Collection.class);
        importHelper.typeRef(List.class);

        builder.append("    @Override\n");
        builder.append("    public Collection<Class<?>> getRootTypes() {\n");
        builder.append("        return List.of();\n"); // todo: fill
        builder.append("    }\n\n");

        builder.append("    @Override\n");
        builder.append("    protected Part createStructure() {\n");
        builder.append("        return null;\n"); // todo: fill
        builder.append("    }\n");
    }

    public String getFileName() {
        return String.format("%s/%s%s", NamingGenerator.getPackagePath(aqr), getClassName(), JavaFileGenerator.JAVA_FILE_EXTENSION);
    }

    private String getPackageName() {
        return NamingGenerator.getPackageName(aqr);
    }

    private String getClassName() {
        return String.format("%sViewType", name);
    }
}
