package tools.vitruv.compmodelcons.generator.backend;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.common.types.JvmEnumerationType;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;
import org.jspecify.annotations.NonNull;
import tools.vitruv.compmodelcons.generator.tools.Metamodel;
import tools.vitruv.neojoin.jvmmodel.SourceModelInferrer;
import tools.vitruv.neojoin.jvmmodel.SourceTypeRegistryInitialization;
import tools.vitruv.neojoin.jvmmodel.TypeRegistry;
import tools.vitruv.neojoin.utils.EMFUtils;

import java.util.LinkedHashSet;
import java.util.Set;

public class GeneratorBackedSourceTypeRegistryInitialization implements SourceTypeRegistryInitialization {
    @Override
    public void initialize(@NonNull TypeRegistry typeRegistry, EPackage.@NonNull Registry registry, @NonNull JvmTypeReferenceBuilder typeReferenceBuilder, @NonNull ResourceSet resourceSet) {
        Set<EPackage> remaining = new LinkedHashSet<>();

        for (EPackage ePackage : EMFUtils.collectAvailablePackages(registry)) {
            Metamodel metamodel = Metamodel.load(ePackage, resourceSet);

            if (metamodel == null) {
                remaining.add(ePackage);
                continue;
            }

            for (EClassifier eClassifier : ePackage.getEClassifiers()) {
                switch (eClassifier) {
                    case EClass eClass -> {
                        String typeName = metamodel.getGenClass(eClass).getQualifiedInterfaceName();
                        JvmType type = typeReferenceBuilder.typeRef(typeName).getType();
                        if (type instanceof JvmGenericType genericType) {
                            typeRegistry.referenceClass(eClass, genericType);
                        }
                    }
                    case EEnum eEnum -> {
                        String typeName = metamodel.getGenEnum(eEnum).getQualifiedInstanceClassName();
                        JvmType type = typeReferenceBuilder.typeRef(typeName).getType();
                        if (type instanceof JvmEnumerationType enumerationType) {
                            typeRegistry.referenceEnum(eEnum, enumerationType);
                        }
                    }
                    default -> {

                    }
                }
            }
        }

        if (!remaining.isEmpty()) {
            new SourceModelInferrer(typeRegistry, remaining, typeReferenceBuilder).infer();
        }
    }
}
