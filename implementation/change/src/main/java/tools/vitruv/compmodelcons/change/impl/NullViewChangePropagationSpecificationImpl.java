package tools.vitruv.compmodelcons.change.impl;

import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.compmodelcons.change.ViewChangePropagationSpecification;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.vsum.VirtualModel;

import java.util.List;

public class NullViewChangePropagationSpecificationImpl implements ViewChangePropagationSpecification {
    private final List<MetamodelDescriptor> metamodelDescriptors;

    public NullViewChangePropagationSpecificationImpl(MetamodelDescriptor metamodelDescriptor) {
        this.metamodelDescriptors = List.of(metamodelDescriptor);
    }

    @Override
    public List<MetamodelDescriptor> getOriginMetamodelDescriptors() {
        return metamodelDescriptors;
    }

    @Override
    public MetamodelDescriptor getViewTypeMetamodelDescriptor() {
        return metamodelDescriptors.get(0);
    }

    @Override
    public View getView(VirtualModel vsum) {
        return null;
    }
}
