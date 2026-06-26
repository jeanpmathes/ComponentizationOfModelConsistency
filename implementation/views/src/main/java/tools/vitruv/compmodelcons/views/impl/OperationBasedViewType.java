package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.atomic.uuid.Uuid;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.compmodelcons.views.operations.Operation;

import java.util.Collection;
import java.util.List;

public abstract class OperationBasedViewType extends TransformingViewType {
    public OperationBasedViewType(String name, EPackage metaModel) {
        super(name, metaModel);
    }

    protected abstract Operation createStructure(Resource model);

    @Override
    protected void generateView(Collection<Resource> sources, List<Resource> target) {
        // todo: implement
    }

    @Override
    protected VitruviusChange<Uuid> transformChange(Collection<Resource> sources, VitruviusChange<HierarchicalId> change) {
        return null; // todo: implement
    }
}
