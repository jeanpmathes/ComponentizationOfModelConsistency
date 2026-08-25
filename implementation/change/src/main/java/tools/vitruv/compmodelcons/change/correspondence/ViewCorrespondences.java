package tools.vitruv.compmodelcons.change.correspondence;

import java.util.List;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

public interface ViewCorrespondences {
  boolean canProvideViewObjectForOriginObjects(List<EObject> originObjects, EClass viewClass);

  EObject getCorrespondingViewObjectForOriginObjects(
      List<EObject> originObjects,
      EClass viewClass);

  boolean canProvideOriginObjectsForViewObject(EObject viewObject);

  List<EObject> getCorrespondingOriginObjectsForViewObject(EObject viewObject);
}
