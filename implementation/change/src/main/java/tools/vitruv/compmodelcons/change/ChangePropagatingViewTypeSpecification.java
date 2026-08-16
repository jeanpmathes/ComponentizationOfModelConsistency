package tools.vitruv.compmodelcons.change;

import java.util.function.Function;
import org.eclipse.emf.common.util.URI;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.propagation.ChangePropagationObservable;
import tools.vitruv.change.utils.ResourceAccess;

/**
 * The specification of a viewtype that can be used for change propagation.
 */
public interface ChangePropagatingViewTypeSpecification {
  MetamodelDescriptor getOriginMetamodelDescriptor();

  MetamodelDescriptor getViewTypeMetamodelDescriptor();

  ChangePropagationView createView(ResourceAccess resourceAccess,
                                   CorrespondenceModelAccess correspondenceModelAccess,
                                   Function<String, URI> uriFactory,
                                   ChangePropagationObservable observable,
                                   ResourceAccess actualResourceAccess);
}
