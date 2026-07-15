package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.compmodelcons.views.Context;
import tools.vitruv.compmodelcons.views.EditableViewCorrespondences;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractContext implements Context {
    private final OriginResourceAccess originResourceAccess;
    private final EditableViewCorrespondences correspondences;

    protected AbstractContext(OriginResourceAccess originResourceAccess, EditableViewCorrespondences correspondences) {
        this.originResourceAccess = originResourceAccess;
        this.correspondences = correspondences;
    }

    protected OriginResourceAccess getOriginResourceAccess() {
        return originResourceAccess;
    }

    @Override
    public List<EObject> getOriginObjects(EClass eClass) {
        List<EObject> result = new ArrayList<>();

        for (Resource resource : originResourceAccess.getResources(eClass.getEPackage())) {
            for (EObject root : resource.getContents()) {
                if (!root.eClass().getEPackage().equals(eClass.getEPackage())) {
                    continue;
                }
                if (eClass.isSuperTypeOf(root.eClass())) {
                    result.add(root);
                }
                var iterator = root.eAllContents();
                while (iterator.hasNext()) {
                    EObject eObject = iterator.next();
                    if (eClass.isSuperTypeOf(eObject.eClass())) {
                        result.add(eObject);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public EditableViewCorrespondences getCorrespondences() {
        return correspondences;
    }
}
