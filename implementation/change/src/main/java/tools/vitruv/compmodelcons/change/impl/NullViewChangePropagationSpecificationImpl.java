package tools.vitruv.compmodelcons.change.impl;

import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.compmodelcons.change.ViewChangePropagationSpecification;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.vsum.VirtualModel;

public class NullViewChangePropagationSpecificationImpl implements ViewChangePropagationSpecification {
    private final MetamodelDescriptor metamodelDescriptor;

    public NullViewChangePropagationSpecificationImpl(MetamodelDescriptor metamodelDescriptor) {
        this.metamodelDescriptor = metamodelDescriptor;
    }

    @Override
    public MetamodelDescriptor getOriginMetamodelDescriptor() {
        return metamodelDescriptor;
    }

    @Override
    public MetamodelDescriptor getViewTypeMetamodelDescriptor() {
        return metamodelDescriptor;
    }

    @Override
    public View getView(VirtualModel vsum) {
        return null;
    }
}
