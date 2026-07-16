package tools.vitruv.compmodelcons.change.impl;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.ChangePropagationView;
import tools.vitruv.compmodelcons.change.ChangePropagationViewTypeSpecification;

import java.util.List;

public class NullViewChangePropagationSpecificationImpl implements ChangePropagationViewTypeSpecification {
    private final MetamodelDescriptor metamodelDescriptor;

    public NullViewChangePropagationSpecificationImpl(MetamodelDescriptor metamodelDescriptor) {
        this.metamodelDescriptor = metamodelDescriptor;
    }

    @Override
    public List<MetamodelDescriptor> getOriginMetamodelDescriptors() {
        return List.of(metamodelDescriptor);
    }

    @Override
    public MetamodelDescriptor getViewTypeMetamodelDescriptor() {
        return metamodelDescriptor;
    }

    @Override
    public ChangePropagationView createView(ResourceAccess resourceAccess) {
        return new DirectModelAccessView(resourceAccess);
    }

    private record DirectModelAccessView(ResourceAccess resourceAccess) implements ChangePropagationView {
        @Override
        public void update() {
            // Because we are working directly on the resource access of the model, there is no need to update.
        }

        @Override
        public ResourceAccess getViewResourceAccess() {
            return resourceAccess;
        }

        @Override
        public List<EChange<EObject>> doGetChange(EChange<EObject> originChange) {
            return List.of(originChange);
        }

        @Override
        public void commit() {
            // Because we are working directly on the resource access of the model, there is no need to commit.
        }

        @Override
        public void close() throws Exception {

        }
    }
}
