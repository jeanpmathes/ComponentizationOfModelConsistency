package tools.vitruv.compmodelcons.views;

import java.util.List;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

public interface Context {
  List<EObject> getOriginObjects(EClass eClass);

  EditableViewCorrespondences getCorrespondences();
}
