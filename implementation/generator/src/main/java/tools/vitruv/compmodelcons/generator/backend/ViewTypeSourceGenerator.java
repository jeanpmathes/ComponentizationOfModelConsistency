package tools.vitruv.compmodelcons.generator.backend;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenClassifier;
import org.eclipse.emf.codegen.ecore.genmodel.GenFeature;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.compmodelcons.change.ChangeSpecificationAwareViewType;
import tools.vitruv.compmodelcons.generator.tools.NamingGenerator;
import tools.vitruv.compmodelcons.views.operations.*;
import tools.vitruv.dsls.common.JavaFileGenerator;
import tools.vitruv.dsls.common.JavaImportHelper;
import tools.vitruv.neojoin.aqr.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
        builder.append("    public static final String NAME = \"").append(name).append("\";\n");

        importHelper.typeRef(List.class);
        importHelper.typeRef(EPackage.class);

        builder.append("    private static final List<EPackage> originMetamodels = List.of(\n");
        for (int index = 0; index < originMetamodels.size(); index++) {
            if (index > 0) {
                builder.append(",\n");
            }
            builder.append("        ").append(getQualifiedPackageInstanceAccessor(originMetamodels.get(index)));
        }
        builder.append("\n");
        builder.append("    );\n");
        builder.append("    private static final EPackage viewtype = ").append(genViewtype.getImportedPackageInterfaceName()).append(".").append(genViewtype.getFactoryInstanceName()).append(";\n\n");

        builder.append("    public ").append(getClassName()).append("() {\n");
        builder.append("        super(NAME, originMetamodels, viewtype);\n");
        builder.append("    }\n\n");

        importHelper.typeRef(MetamodelDescriptor.class);

        builder.append("    @Override\n");
        builder.append("    public List<MetamodelDescriptor> getOriginMetamodelDescriptors() {\n");
        builder.append("        return originMetamodels.stream().map(MetamodelDescriptor::of).toList();\n");
        builder.append("    }\n\n");

        builder.append("    @Override\n");
        builder.append("    public MetamodelDescriptor getViewTypeMetamodelDescriptor() {\n");
        builder.append("        return MetamodelDescriptor.of(viewtype);\n");
        builder.append("    }\n\n");

        importHelper.typeRef(Root.class);

        builder.append("    @Override\n");
        builder.append("    protected Root createStructure() {\n");
        appendRootOperation(builder);
        builder.append("    }\n");
    }

    private void appendRootOperation(StringBuilder builder) {
        importHelper.typeRef(Root.class);
        importHelper.typeRef(Optional.class);
        importHelper.typeRef(List.class);

        GenClass rootClass = getGenClass(aqr.root().name());

        builder.append("        return new Root(\n");
        builder.append("            ").append(rootClass.getQualifiedClassifierAccessor()).append(",\n");

        if (aqr.root().source() == null) {
            builder.append("            ").append("Optional.empty(),\n");
        } else {
            builder.append("            ").append("Optional.of(\n");
            appendProjectOperation(builder, 1, aqr.root());
            builder.append("            ),\n");
        }

        builder.append("            List.of(");
        boolean first = true;
        for (AQRFeature feature : aqr.root().features()) {
            if (feature instanceof AQRFeature.Reference reference && feature.kind() instanceof AQRFeature.Kind.Generate) {
                builder.append(first ? "\n" : ",\n");
                first = false;

                GenFeature containment = getGenFeature(rootClass.getEcoreClass(), reference.name());

                builder.append("                ").append("new Root.Target(\n");
                builder.append("                    ").append(containment.getQualifiedFeatureAccessor()).append(",\n");
                appendProjectOperation(builder, 2, reference.type());
                builder.append("                )");
            }
        }
        if (!first) {
            builder.append("\n").append("            ");
        }
        builder.append(")\n");

        builder.append("        );\n");
    }

    private void appendProjectOperation(StringBuilder builder, int level, AQRTargetClass target) {
        importHelper.typeRef(Project.class);

        GenClass targetClass = getGenClass(target.name());

        builder.append(indent(level)).append("new Project(\n");
        builder.append(indent(level + 1)).append(targetClass.getQualifiedClassifierAccessor()).append(",\n");
        appendQueryOperations(builder, level + 1, Objects.requireNonNull(target.source()));

        builder.append(",\n").append(indent(level + 1)).append("List.of(");
        boolean first = true;
        for (AQRFeature feature : target.features()) {
            if (feature.kind() instanceof AQRFeature.Kind.Generate) {
                continue;
            }

            builder.append(first ? "\n" : ",\n");
            first = false;

            appendFeatureProjectOperation(builder, level + 2, targetClass, feature);
        }
        if (!first) {
            builder.append("\n").append(indent(level + 1));
        }
        builder.append(")\n");

        builder.append(indent(level)).append(")\n");
    }

    private void appendQueryOperations(StringBuilder builder, int level, AQRSource source) {
        appendSourceOperation(builder, level, source.from());
    }

    private void appendSourceOperation(StringBuilder builder, int level, AQRFrom from) {
        importHelper.typeRef(Source.class);

        builder.append(indent(level)).append("new Source(").append(getQualifiedClassInstanceAccessor(from.clazz())).append(")");
    }

    private void appendFeatureProjectOperation(StringBuilder builder, int level, GenClass targetClass, AQRFeature feature) {
        GenFeature createdFeature = getGenFeature(targetClass.getEcoreClass(), feature.name());

        importHelper.typeRef(FeatureProject.class);
        importHelper.typeRef(Optional.class);

        builder.append(indent(level)).append("new FeatureProject(\n");
        if (feature.kind() instanceof AQRFeature.Kind.Copy copy) {
            builder.append(indent(level + 1)).append("Optional.of(").append(getQualifiedFeatureInstanceAccessor(copy.source())).append("),\n");
        } else {
            builder.append(indent(level + 1)).append("Optional.of()").append(",\n");
        }
        builder.append(indent(level + 1)).append(createdFeature.getQualifiedFeatureAccessor()).append(",\n");
        if (feature.kind() instanceof AQRFeature.Kind.Copy copy) {
            appendFeatureSourceOperation(builder, level + 1, copy);
            builder.append("\n");
        } else {
            throw new UnsupportedOperationException();
        }
        builder.append(indent(level)).append(")");
    }

    private void appendFeatureSourceOperation(StringBuilder builder, int level, AQRFeature.Kind.Copy copy) {
        importHelper.typeRef(FeatureSource.class);

        builder.append(indent(level)).append("new FeatureSource(").append(getQualifiedFeatureInstanceAccessor(copy.source())).append(")");
    }

    private String indent(int indent) {
        return "    ".repeat(indent + 3);
    }

    private GenClass getGenClass(String name) {
        return (GenClass) getGenClassifier(name);
    }

    private GenClass getGenClass(EClass eClass) {
        return (GenClass) getGenClassifier(eClass);
    }

    private GenClassifier getGenClassifier(String name) {
        return getGenClassifier(viewtype.getEClassifier(name));
    }

    private GenClassifier getGenClassifier(EClassifier eClassifier) {
        return genViewtype.getGenClassifiers().stream().filter(classifier -> classifier.getEcoreClassifier().equals(eClassifier)).findAny().orElseThrow();
    }

    private GenFeature getGenFeature(EClass eClass, String name) {
        EStructuralFeature eStructuralFeature = eClass.getEStructuralFeature(name);
        return getGenClass(eClass).getAllGenFeatures().stream().filter(feature -> feature.getEcoreFeature().equals(eStructuralFeature)).findAny().orElseThrow();
    }

    private String getQualifiedPackageInstanceAccessor(EPackage ePackage) {
        importHelper.typeRef(EPackage.class);

        return "EPackage.Registry.INSTANCE.getEPackage(\"" + StringEscapeUtils.escapeJava(ePackage.getNsURI()) + "\")";
    }

    private String getQualifiedClassInstanceAccessor(EClass eClass) {
        importHelper.typeRef(EClass.class);

        return "(EClass)" + getQualifiedPackageInstanceAccessor(eClass.getEPackage()) + ".getEClassifier(\"" + StringEscapeUtils.escapeJava(eClass.getName()) + "\")";
    }

    private String getQualifiedFeatureInstanceAccessor(EStructuralFeature eStructuralFeature) {
        return "(" + getQualifiedClassInstanceAccessor(eStructuralFeature.getEContainingClass()) + ").getEStructuralFeature(\"" + StringEscapeUtils.escapeJava(eStructuralFeature.getName()) + "\")";
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
