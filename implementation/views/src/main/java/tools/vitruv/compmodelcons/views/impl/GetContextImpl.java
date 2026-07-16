package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.compmodelcons.views.EditableViewCorrespondences;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;
import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;

public class GetContextImpl extends AbstractContext implements GetContext {
    private final ViewResourceAccess viewResourceAccess;

    public GetContextImpl(OriginResourceAccess originResourceAccess, ViewResourceAccess viewResourceAccess, EditableViewCorrespondences correspondences) {
        super(originResourceAccess, correspondences);
        this.viewResourceAccess = viewResourceAccess;
    }

    @Override
    public void insertViewRoot(EObject root) {
        viewResourceAccess.insertRoot(root);
    }
}
