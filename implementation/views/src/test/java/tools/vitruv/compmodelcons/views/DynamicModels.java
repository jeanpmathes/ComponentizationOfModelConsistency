package tools.vitruv.compmodelcons.views;

import org.eclipse.emf.ecore.*;

public class DynamicModels {
    private DynamicModels() {

    }

    public static EPackage createEPackage() {
        return EcoreFactory.eINSTANCE.createEPackage();
    }

    public static EClass createEClass(EPackage ePackage) {
        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        ePackage.getEClassifiers().add(eClass);
        return eClass;
    }

    public static EReference createContainmentEReference(EClass eClass, String name, EClass eReferenceType) {
        EReference eReference = EcoreFactory.eINSTANCE.createEReference();
        eReference.setName(name);
        eReference.setContainment(true);
        eReference.setUpperBound(-1);
        eReference.setEType(eReferenceType);

        eClass.getEStructuralFeatures().add(eReference);

        return eReference;
    }

    public static EObject createEObject(EClass eClass) {
        return eClass.getEPackage().getEFactoryInstance().create(eClass);
    }

    public static EClass getEClass(EPackage ePackage, String name) {
        return (EClass) ePackage.getEClassifier(name);
    }
}
