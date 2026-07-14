package tools.vitruv.compmodelcons.change;

import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.utils.ResourceAccess;

import java.util.List;

public interface ChangePropagationViewTypeSpecification {
    List<MetamodelDescriptor> getOriginMetamodelDescriptors();

    MetamodelDescriptor getViewTypeMetamodelDescriptor();

    ChangePropagationView createView(ResourceAccess resourceAccess);
}
