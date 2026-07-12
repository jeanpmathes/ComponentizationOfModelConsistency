package tools.vitruv.compmodelcons.views;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.*;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.EObjectAddedEChange;
import tools.vitruv.change.atomic.eobject.EObjectExistenceEChange;
import tools.vitruv.change.atomic.eobject.EObjectSubtractedEChange;
import tools.vitruv.change.atomic.feature.FeatureEChange;

import java.util.HashSet;
import java.util.Set;

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

    public static EClass createEClass(EPackage ePackage, String name) {
        EClass eClass = createEClass(ePackage);
        eClass.setName(name);
        return eClass;
    }

    public static EReference createManyContainmentEReference(EClass eClass, String name, EClass eReferenceType) {
        EReference eReference = createEReference(eClass, name, eReferenceType);
        eReference.setUpperBound(-1);
        eReference.setContainment(true);
        return eReference;
    }

    public static EReference createEReference(EClass eClass, String name, EClass eReferenceType) {
        EReference eReference = EcoreFactory.eINSTANCE.createEReference();
        eReference.setName(name);
        eReference.setEType(eReferenceType);
        eClass.getEStructuralFeatures().add(eReference);
        return eReference;
    }

    public static EAttribute createEAttribute(EClass eClass, String name, EDataType eDataType) {
        EAttribute eAttribute = EcoreFactory.eINSTANCE.createEAttribute();
        eAttribute.setName(name);
        eAttribute.setEType(eDataType);
        eClass.getEStructuralFeatures().add(eAttribute);
        return eAttribute;
    }

    public static EObject createEObject(EClass eClass) {
        return eClass.getEPackage().getEFactoryInstance().create(eClass);
    }

    public static EClass getEClass(EPackage ePackage, String name) {
        return (EClass) ePackage.getEClassifier(name);
    }

    public static EAttribute getEAttribute(EClass target, String name) {
        return (EAttribute) target.getEStructuralFeature(name);
    }

    public static EReference getEReference(EClass target, String name) {
        return (EReference) target.getEStructuralFeature(name);
    }

    public static EList<EObject> getList(EObject target, EReference reference) {
        //noinspection unchecked
        return (EList<EObject>) target.eGet(reference);
    }

    public static EObject getAffectedEObject(EChange<EObject> eChange) {
        if (eChange instanceof EObjectExistenceEChange<EObject> eObjectEObjectExistenceEChange) {
            return eObjectEObjectExistenceEChange.getAffectedElement();
        } else if (eChange instanceof FeatureEChange<EObject, ?> featureEChange) {
            return featureEChange.getAffectedElement();
        } else if (eChange instanceof EObjectAddedEChange<EObject> eObjectEObjectAddedEChange) {
            return eObjectEObjectAddedEChange.getNewValue();
        } else if (eChange instanceof EObjectSubtractedEChange<EObject> eObjectEObjectSubtractedEChange) {
            return eObjectEObjectSubtractedEChange.getOldValue();
        } else {
            throw new IllegalArgumentException("Unknown change type: " + eChange.getClass().getSimpleName());
        }
    }

    public static boolean isRoot(EClass sourceClass) {
        for (EClassifier eClassifier : sourceClass.getEPackage().getEClassifiers()) {
            if (eClassifier instanceof EClass eClass) {
                if (eClass.isSuperTypeOf(sourceClass)) {
                    continue;
                }
                for (EReference eReference : eClass.getEReferences()) {
                    if (eReference.isContainment() && eReference.getEReferenceType().isSuperTypeOf(sourceClass)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static EReference getUnambiguousContainer(EClass sourceClass) {
        Set<EReference> containers = new HashSet<>();

        for (EClassifier eClassifier : sourceClass.getEPackage().getEClassifiers()) {
            if (eClassifier instanceof EClass eClass) {
                for (EReference eReference : eClass.getEReferences()) {
                    if (eReference.isContainment() && eReference.isMany() && eReference.getEReferenceType().isSuperTypeOf(sourceClass)) {
                        containers.add(eReference);
                    }
                }
            }
        }

        if (containers.size() != 1) {
            return null;
        }

        return containers.iterator().next();
    }
}
