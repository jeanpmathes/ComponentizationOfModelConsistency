package tools.vitruv.compmodelcons.change.correspondence.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceObjectViewObjectTranslator;

public class PassthroughCorrespondenceObjectViewObjectTranslatorImpl
    implements CorrespondenceObjectViewObjectTranslator {
  private final MetamodelDescriptor metamodel;

  public PassthroughCorrespondenceObjectViewObjectTranslatorImpl(MetamodelDescriptor metamodel) {
    this.metamodel = metamodel;
  }

  @Override
  public boolean canTranslateViewEObject(EObject viewObject) {
    return isPartOfMetamodel(viewObject
                                 .eClass());
  }

  private boolean isPartOfMetamodel(EClass eClass) {
    return metamodel
               .getNsUris()
               .contains(eClass.getEPackage().getNsURI());
  }

  @Override
  public boolean canTranslateCorrespondenceEObject(EObject correspondenceObject) {
    return isPartOfMetamodel(correspondenceObject
                                 .eClass());
  }

  @Override
  public EObject translateViewEObject(EObject correspondenceEObject) {
    return correspondenceEObject;
  }

  @Override
  public EObject translateCorrespondenceEObject(EObject viewEObject, boolean createIfNotExist) {
    return viewEObject;
  }

  @Override
  public void onViewFitted() {

  }

  @Override
  public void onResolverUse() {

  }

  @Override
  public void close() {

  }
}
