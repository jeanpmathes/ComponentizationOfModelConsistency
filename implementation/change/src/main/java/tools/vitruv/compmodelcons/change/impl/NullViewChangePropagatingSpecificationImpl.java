package tools.vitruv.compmodelcons.change.impl;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.propagation.ChangePropagationObservable;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.*;

import java.util.List;
import java.util.function.Function;

public class NullViewChangePropagatingSpecificationImpl implements ChangePropagatingViewTypeSpecification {
    private final MetamodelDescriptor metamodelDescriptor;

    public NullViewChangePropagatingSpecificationImpl(MetamodelDescriptor metamodelDescriptor) {
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
    public ChangePropagationView createView(ResourceAccess resourceAccess, CorrespondenceModelAccess correspondenceModelAccess, Function<String, URI> uriFactory, ChangePropagationObservable observable, CorrespondenceResolvingContext correspondenceContext) {
        return new DirectModelAccessView(metamodelDescriptor, resourceAccess);
    }

    private record DirectModelAccessView(MetamodelDescriptor metamodel,
                                         ResourceAccess resourceAccess) implements ChangePropagationView {
        @Override
        public ResourceAccess getViewResourceAccess() {
            return resourceAccess;
        }

        @Override
        public CorrespondenceResolver getCorrespondenceResolver() {
            return new PassthroughCorrespondenceResolverImpl(metamodel);
        }

        @Override
        public void commit() {
            // Because we are working directly on the resource access of the model, there is no need to commit.
        }

        @Override
        public List<EChange<EObject>> fitAndDetermineChanges(ResourceAccess changedOrigin, CorrespondenceModelAccess changedCorrespondenceModel, List<EChange<EObject>> originChanges, ChangeDeterminationMode changeDeterminationMode) {
            return originChanges;
        }

        @Override
        public void close() throws Exception {

        }
    }
}
