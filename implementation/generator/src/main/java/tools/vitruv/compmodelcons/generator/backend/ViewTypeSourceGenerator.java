package tools.vitruv.compmodelcons.generator.backend;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.compmodelcons.change.ChangeSpecificationAwareViewType;
import tools.vitruv.compmodelcons.generator.tools.NamingGenerator;
import tools.vitruv.compmodelcons.views.operations.Operation;
import tools.vitruv.dsls.common.JavaFileGenerator;
import tools.vitruv.dsls.common.JavaImportHelper;
import tools.vitruv.neojoin.aqr.AQR;

import java.util.Collection;
import java.util.List;

public class ViewTypeSourceGenerator {
    private final JavaImportHelper importHelper = new JavaImportHelper();

    private final String name;
    private final AQR aqr;
    private final List<EPackage> originMetamodels;
    private final EPackage viewtype;
    private final GenPackage genViewtype;

    public ViewTypeSourceGenerator(String name, List<EPackage> originMetamodels, EPackage viewtype, GenPackage genViewtype, AQR aqr) {
        this.name = NamingGenerator.convertToPascalCase(name);
        this.aqr = aqr;
        this.originMetamodels = originMetamodels;
        this.viewtype = viewtype;
        this.genViewtype = genViewtype;
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

        importHelper.typeRef(List.class);
        importHelper.typeRef(EPackage.class);
        importHelper.typeRef(MetamodelDescriptor.class);

        builder.append("    private static final List<MetamodelDescriptor> originMetamodels = List.of(\n");
        for (int i = 0; i < originMetamodels.size(); i++) {
            if (i > 0) {
                builder.append(",\n");
            }
            builder.append("        MetamodelDescriptor.of(").append(getQualifiedPackageInstanceAccessor(originMetamodels.get(i))).append(")");
        }
        builder.append("    );\n");
        builder.append("    private static final EPackage viewtype = ").append(getQualifiedPackageInstanceAccessor(genViewtype)).append(";\n\n");

        builder.append("    public ").append(getClassName()).append("() {\n");
        builder.append("        super(name, viewtype);\n");
        builder.append("    }\n\n");

        importHelper.typeRef(Collection.class);
        importHelper.typeRef(MetamodelDescriptor.class);

        builder.append("    @Override\n");
        builder.append("    public List<MetamodelDescriptor> getOriginMetamodelDescriptors() {\n");
        builder.append("        return originMetamodels;\n");
        builder.append("    }\n\n");

        builder.append("    @Override\n");
        builder.append("    public MetamodelDescriptor getViewTypeMetamodelDescriptor() {\n");
        builder.append("        return MetamodelDescriptor.of(viewtype);\n");
        builder.append("    }\n\n");

        importHelper.typeRef(Collection.class);
        importHelper.typeRef(List.class);

        builder.append("    @Override\n");
        builder.append("    public Collection<Class<?>> getRootTypes() {\n");
        builder.append("        return List.of();\n"); // todo: fill
        builder.append("    }\n\n");

        importHelper.typeRef(Operation.class);

        builder.append("    @Override\n");
        builder.append("    protected Operation createStructure() {\n");
        builder.append("        return null;\n"); // todo: fill
        builder.append("    }\n");
    }

    private String getQualifiedPackageInstanceAccessor(GenPackage genPackage) {
        return genPackage.getQualifiedPackageInterfaceName() + ".eINSTANCE";
    }

    private String getQualifiedPackageInstanceAccessor(EPackage ePackage) {
        importHelper.typeRef(EPackage.class);

        return "EPackage.Registry.INSTANCE.getEPackage(\"" + StringEscapeUtils.escapeJava(ePackage.getNsURI()) + "\")";
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
