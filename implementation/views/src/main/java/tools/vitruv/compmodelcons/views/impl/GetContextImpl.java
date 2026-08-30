package tools.vitruv.compmodelcons.views.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.compmodelcons.views.EditableViewCorrespondences;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;
import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;

public final class GetContextImpl extends AbstractGetContext {
  private final Map<EClass, List<EObject>> originObjectsByClass = new HashMap<>();

  public GetContextImpl(OriginResourceAccess originResourceAccess,
                        ViewResourceAccess viewResourceAccess,
                        EditableViewCorrespondences correspondences) {
    super(originResourceAccess, viewResourceAccess, correspondences);
  }

  @Override
  public List<EObject> getOriginObjects(EClass eClass) {
    return originObjectsByClass.computeIfAbsent(eClass, super::getOriginObjects);
  }
}
