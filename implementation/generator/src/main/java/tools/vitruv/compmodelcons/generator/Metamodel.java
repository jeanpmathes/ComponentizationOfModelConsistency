package tools.vitruv.compmodelcons.generator;

import org.eclipse.emf.codegen.ecore.genmodel.*;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.util.List;

public record Metamodel(EPackage ePackage, GenPackage genPackage) {
    public static Metamodel load(EPackage ePackage, ResourceSet resourceSet) {
        URI genModelUri = EcorePlugin.getEPackageNsURIToGenModelLocationMap(true).get(ePackage.getNsURI());

        if (genModelUri.isPlatformResource()) {
            genModelUri = EcorePlugin.resolvePlatformResourcePath(genModelUri.toPlatformString(true));
        }

        Resource resource = resourceSet.getResource(genModelUri, true);
        GenModel loadedGenModel = (GenModel) resource.getContents().get(0);

        GenModel reconciledGenModel = GenModelFactory.eINSTANCE.createGenModel();
        reconciledGenModel.initialize(List.of(ePackage));

        reconciledGenModel.reconcile(loadedGenModel);

        GenPackage genPackage = reconciledGenModel.findGenPackage(ePackage);

        return new Metamodel(ePackage, genPackage);
    }

    public String getFullyQualifiedPackageInterfaceAccessor() {
        return genPackage.getImportedPackageInterfaceName() + "." + genPackage.getFactoryInstanceName();
    }

    public GenClassifier getGenClassifier(String name) {
        return getGenClassifier(ePackage.getEClassifier(name));
    }

    public GenClassifier getGenClassifier(EClassifier eClassifier) {
        return genPackage.getGenClassifiers().stream().filter(classifier -> classifier.getEcoreClassifier().equals(eClassifier)).findAny().orElseThrow();
    }

    public GenClass getGenClass(String name) {
        return (GenClass) getGenClassifier(name);
    }

    public GenClass getGenClass(EClass eClass) {
        return (GenClass) getGenClassifier(eClass);
    }

    public GenFeature getGenFeature(EClass eClass, String name) {
        return getGenFeature(eClass.getEStructuralFeature(name));
    }

    public GenFeature getGenFeature(EStructuralFeature eStructuralFeature) {
        return getGenClass(eStructuralFeature.getEContainingClass()).getAllGenFeatures().stream().filter(feature -> feature.getEcoreFeature().equals(eStructuralFeature)).findAny().orElseThrow();
    }
}
