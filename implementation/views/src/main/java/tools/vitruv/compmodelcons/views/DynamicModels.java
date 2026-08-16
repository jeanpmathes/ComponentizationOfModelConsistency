package tools.vitruv.compmodelcons.views;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.EObjectAddedEChange;
import tools.vitruv.change.atomic.eobject.EObjectExistenceEChange;
import tools.vitruv.change.atomic.eobject.EObjectSubtractedEChange;
import tools.vitruv.change.atomic.feature.FeatureEChange;

/**
 * Utilities for working with dynamic EMF models.
 */
public class DynamicModels {
  private DynamicModels() {

  }

  /**
   * Creates a new EPackage.
   *
   * @return the new EPackage
   */
  public static EPackage createEPackage() {
    return EcoreFactory.eINSTANCE.createEPackage();
  }

  /**
   * Creates a new EClass in the given EPackage.
   *
   * @param ePackage the EPackage to create the EClass in
   * @return the new EClass
   */
  public static EClass createEClass(EPackage ePackage) {
    EClass eClass = EcoreFactory.eINSTANCE.createEClass();
    ePackage
        .getEClassifiers()
        .add(eClass);
    return eClass;
  }

  /**
   * Creates a new EClass in the given EPackage with the given name.
   *
   * @param ePackage the EPackage to create the EClass in
   * @param name     the name of the EClass
   * @return the new EClass
   */
  public static EClass createEClass(EPackage ePackage, String name) {
    EClass eClass = createEClass(ePackage);
    eClass.setName(name);
    return eClass;
  }

  /**
   * Creates a new containment EReference with the given name that can contain multiple instances.
   *
   * @param eClass         the EClass to create the EReference in
   * @param name           the name of the EReference
   * @param eReferenceType the type of the contained objects
   * @return the new EReference
   */
  public static EReference createManyContainmentEReference(EClass eClass, String name,
                                                           EClass eReferenceType) {
    EReference eReference = createEReference(eClass, name, eReferenceType);
    eReference.setUpperBound(-1);
    eReference.setContainment(true);
    return eReference;
  }

  /**
   * Creates a new EReference with the given name.
   *
   * @param eClass         the EClass to create the EReference in
   * @param name           the name of the EReference
   * @param eReferenceType the type of the contained object
   * @return the new EReference
   */
  public static EReference createEReference(EClass eClass, String name, EClass eReferenceType) {
    EReference eReference = EcoreFactory.eINSTANCE.createEReference();
    eReference.setName(name);
    eReference.setEType(eReferenceType);
    eClass
        .getEStructuralFeatures()
        .add(eReference);
    return eReference;
  }

  /**
   * Creates a new EAttribute with the given name and data type.
   *
   * @param eClass    the EClass to create the EAttribute in
   * @param name      the name of the EAttribute
   * @param eDataType the data type of the EAttribute
   * @return the new EAttribute
   */
  public static EAttribute createEAttribute(EClass eClass, String name, EDataType eDataType) {
    EAttribute eAttribute = EcoreFactory.eINSTANCE.createEAttribute();
    eAttribute.setName(name);
    eAttribute.setEType(eDataType);
    eClass
        .getEStructuralFeatures()
        .add(eAttribute);
    return eAttribute;
  }

  /**
   * Creates a new EObject of the given EClass.
   *
   * @param eClass the EClass of the EObject to create
   * @return the new EObject
   */
  public static EObject createEObject(EClass eClass) {
    return eClass
        .getEPackage()
        .getEFactoryInstance()
        .create(eClass);
  }

  /**
   * Get an EClass by name from the given EPackage.
   *
   * @param ePackage the EPackage to get the EClass from
   * @param name     the name of the EClass
   * @return the EClass
   */
  public static EClass getEClass(EPackage ePackage, String name) {
    return (EClass) ePackage.getEClassifier(name);
  }

  /**
   * Get an EAttribute by name from the given EClass.
   *
   * @param target the EClass to get the EAttribute from
   * @param name   the name of the EAttribute
   * @return the EAttribute
   */
  public static EAttribute getEAttribute(EClass target, String name) {
    return (EAttribute) target.getEStructuralFeature(name);
  }

  /**
   * Get an EReference by name from the given EClass.
   *
   * @param target the EClass to get the EReference from
   * @param name   the name of the EReference
   * @return the EReference
   */
  public static EReference getEReference(EClass target, String name) {
    return (EReference) target.getEStructuralFeature(name);
  }

  /**
   * Get the list of a many-valued EReference of the given EObject.
   *
   * @param target    the EObject to get the list from
   * @param reference the EReference to get the list for
   * @return the list of values
   */
  public static EList<EObject> getList(EObject target, EReference reference) {
    //noinspection unchecked
    return (EList<EObject>) target.eGet(reference);
  }

  /**
   * Get the EObject that is affected by the given change.
   *
   * @param eChange the change to get the affected EObject for
   * @return the EObject
   */
  public static EObject getAffectedEObject(EChange<EObject> eChange) {
    return switch (eChange) {
      case EObjectExistenceEChange<EObject> eObjectEObjectExistenceEChange ->
          eObjectEObjectExistenceEChange.getAffectedElement();
      case FeatureEChange<EObject, ?> featureEChange -> featureEChange.getAffectedElement();
      case EObjectAddedEChange<EObject> eObjectEObjectAddedEChange ->
          eObjectEObjectAddedEChange.getNewValue();
      case EObjectSubtractedEChange<EObject> eObjectEObjectSubtractedEChange ->
          eObjectEObjectSubtractedEChange.getOldValue();
      default -> throw new IllegalArgumentException("Unknown change type: " + eChange
          .getClass()
          .getSimpleName());
    };
  }

  /**
   * Check whether a class is a root class within a model. A class is a root class if there is no
   * containment reference in the package that can contain instances of the class.
   *
   * @param sourceClass the class to check
   * @return true if the class is a root class, false otherwise
   */
  public static boolean isRoot(EClass sourceClass) {
    for (EClassifier eClassifier : sourceClass
        .getEPackage()
        .getEClassifiers()) {
      if (eClassifier instanceof EClass eClass) {
        if (eClass.isSuperTypeOf(sourceClass)) {
          continue;
        }
        for (EReference eReference : eClass.getEReferences()) {
          if (eReference.isContainment() && eReference
              .getEReferenceType()
              .isSuperTypeOf(sourceClass)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * Attempts to find an unambiguous containment reference for the given class.
   *
   * @param sourceClass the class for which to find a containment reference
   * @return the unambiguous containment reference, or null if none exists
   */
  public static EReference getUnambiguousContainer(EClass sourceClass) {
    Set<EReference> containers = new HashSet<>();

    for (EClassifier eClassifier : sourceClass
        .getEPackage()
        .getEClassifiers()) {
      if (eClassifier instanceof EClass eClass) {
        for (EReference eReference : eClass.getEReferences()) {
          if (eReference.isContainment() && eReference.isMany() && eReference
              .getEReferenceType()
              .isSuperTypeOf(sourceClass)) {
            containers.add(eReference);
          }
        }
      }
    }

    if (containers.size() != 1) {
      return null;
    }

    return containers
        .iterator()
        .next();
  }
}
