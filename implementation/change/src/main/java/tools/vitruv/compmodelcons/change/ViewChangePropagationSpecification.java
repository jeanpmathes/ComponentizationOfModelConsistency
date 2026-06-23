package tools.vitruv.compmodelcons.change;

import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.vsum.VirtualModel;

public interface ViewChangePropagationSpecification {
    MetamodelDescriptor getOriginMetamodelDescriptor();

    MetamodelDescriptor getViewTypeMetamodelDescriptor();

    View getView(VirtualModel vsum);
}
