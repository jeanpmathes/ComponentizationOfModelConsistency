package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.common.util.URI;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.utils.ResourceAccess;

import java.util.List;
import java.util.function.Function;

public interface ChangePropagationViewTypeSpecification {
    List<MetamodelDescriptor> getOriginMetamodelDescriptors();

    MetamodelDescriptor getViewTypeMetamodelDescriptor();

    ChangePropagationView createView(int originMetamodelIndex, ResourceAccess resourceAccess, Function<String, URI> uriFactory, CorrespondenceResolvingContext correspondenceContext);
}
