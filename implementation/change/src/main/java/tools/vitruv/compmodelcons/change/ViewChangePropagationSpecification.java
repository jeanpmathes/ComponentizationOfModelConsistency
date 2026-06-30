package tools.vitruv.compmodelcons.change;

import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.vsum.VirtualModel;

import java.util.List;

public interface ViewChangePropagationSpecification {
    List<MetamodelDescriptor> getOriginMetamodelDescriptors();

    MetamodelDescriptor getViewTypeMetamodelDescriptor();

    View getView(VirtualModel vsum);
}
