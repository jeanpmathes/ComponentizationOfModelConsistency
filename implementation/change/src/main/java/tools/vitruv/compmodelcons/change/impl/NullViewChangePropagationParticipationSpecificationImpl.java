package tools.vitruv.compmodelcons.change.impl;

import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.compmodelcons.change.ViewChangePropagationParticipationSpecification;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.vsum.VirtualModel;

public class NullViewChangePropagationParticipationSpecificationImpl implements ViewChangePropagationParticipationSpecification {
    private final MetamodelDescriptor metamodelDescriptor;

    public NullViewChangePropagationParticipationSpecificationImpl(MetamodelDescriptor metamodelDescriptor) {
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
