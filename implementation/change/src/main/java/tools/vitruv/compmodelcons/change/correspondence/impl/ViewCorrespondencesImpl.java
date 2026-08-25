package tools.vitruv.compmodelcons.change.correspondence.impl;

import java.util.List;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.compmodelcons.change.correspondence.ViewCorrespondences;

public class ViewCorrespondencesImpl implements ViewCorrespondences {
  private final MetamodelDescriptor viewTypeMetamodel;
  private final MetamodelDescriptor originMetamodel;
  private final tools.vitruv.compmodelcons.views.ViewCorrespondences inner;

  public ViewCorrespondencesImpl(
      MetamodelDescriptor viewTypeMetamodel, MetamodelDescriptor originMetamodel,
      tools.vitruv.compmodelcons.views.ViewCorrespondences inner) {
    this.viewTypeMetamodel = viewTypeMetamodel;
    this.originMetamodel = originMetamodel;
    this.inner = inner;
  }

  @Override
  public boolean canProvideViewObjectForOriginObjects(
      List<EObject> originObjects,
      EClass viewClass) {
    return originObjects.stream()
               .allMatch(originObject -> isPartOfOriginMetamodel(originObject.eClass()));
  }

  private boolean isPartOfOriginMetamodel(EClass eClass) {
    return originMetamodel.getNsUris().contains(eClass.getEPackage().getNsURI());
  }

  @Override
  public EObject getCorrespondingViewObjectForOriginObjects(
      List<EObject> originObjects,
      EClass viewClass) {
    return inner.getCorrespondingViewObjectForOriginObjects(originObjects, viewClass);
  }

  @Override
  public boolean canProvideOriginObjectsForViewObject(EObject viewObject) {
    return isPartOfViewTypeMetamodel(viewObject.eClass());
  }

  private boolean isPartOfViewTypeMetamodel(EClass eClass) {
    return viewTypeMetamodel.getNsUris().contains(eClass.getEPackage().getNsURI());
  }

  @Override
  public List<EObject> getCorrespondingOriginObjectsForViewObject(EObject viewObject) {
    return inner.getCorrespondingOriginObjectsForViewObject(viewObject);
  }
}
