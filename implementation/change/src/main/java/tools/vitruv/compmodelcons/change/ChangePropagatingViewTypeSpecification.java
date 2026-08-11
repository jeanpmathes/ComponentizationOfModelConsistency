package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.common.util.URI;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.utils.ResourceAccess;

import java.util.List;
import java.util.function.Function;

/**
 * The specification of a viewtype that can be used for change propagation.
 */
public interface ChangePropagatingViewTypeSpecification {
    List<MetamodelDescriptor> getOriginMetamodelDescriptors();

    MetamodelDescriptor getViewTypeMetamodelDescriptor();

    ChangePropagationView createView(int originMetamodelIndex, ResourceAccess resourceAccess, CorrespondenceModelAccess correspondenceModelAccess, Function<String, URI> uriFactory, CorrespondenceResolvingContext correspondenceContext);
}
