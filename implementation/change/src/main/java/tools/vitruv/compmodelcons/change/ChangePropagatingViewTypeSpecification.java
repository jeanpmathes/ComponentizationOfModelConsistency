package tools.vitruv.compmodelcons.change;

import java.util.function.Function;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.propagation.ChangePropagationObservable;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceObjectViewObjectTranslatorFactory;

/**
 * The specification of a viewtype that can be used for change propagation.
 */
public interface ChangePropagatingViewTypeSpecification {
  MetamodelDescriptor getOriginMetamodelDescriptor();

  MetamodelDescriptor getViewTypeMetamodelDescriptor();

  EPackage getMetamodel();

  ChangePropagationView createView(ResourceAccess resourceAccess,
                                   CorrespondenceModelAccess correspondenceModelAccess,
                                   Function<String, URI> uriFactory,
                                   ChangePropagationObservable observable,
                                   CorrespondenceObjectViewObjectTranslatorFactory correspondenceObjectViewObjectTranslatorFactory);
}
