package tools.vitruv.compmodelcons.change.impl;

import java.util.List;
import java.util.function.Function;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.propagation.ChangePropagationObservable;
import tools.vitruv.change.propagation.ModelRepositorySnapshot;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.ChangePropagatingViewTypeSpecification;
import tools.vitruv.compmodelcons.change.ChangePropagationView;
import tools.vitruv.compmodelcons.change.CorrespondenceModelAccess;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceObjectViewObjectTranslator;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceObjectViewObjectTranslatorFactory;
import tools.vitruv.compmodelcons.change.correspondence.impl.PassthroughCorrespondenceObjectViewObjectTranslatorImpl;

public class NullViewChangePropagatingSpecificationImpl
    implements ChangePropagatingViewTypeSpecification {
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
  public ChangePropagationView createView(ResourceAccess resourceAccess,
                                          CorrespondenceModelAccess correspondenceModelAccess,
                                          Function<String, URI> uriFactory,
                                          ChangePropagationObservable observable,
                                          CorrespondenceObjectViewObjectTranslatorFactory correspondenceObjectViewObjectTranslatorFactory) {
    return new DirectModelAccessView(metamodelDescriptor, resourceAccess);
  }

  private static final class DirectModelAccessView implements ChangePropagationView {
    private final MetamodelDescriptor metamodel;
    private ResourceAccess resourceAccess;

    private DirectModelAccessView(MetamodelDescriptor metamodel, ResourceAccess resourceAccess) {
      this.metamodel = metamodel;
      this.resourceAccess = resourceAccess;
    }

    @Override
    public ResourceAccess getViewResourceAccess() {
      return resourceAccess;
    }

    @Override
    public ModelRepositorySnapshot createSnapshot() {
      // On the source side, the existing snapshot is enough and also passed as resource access.
      // On the target side, this operation is currently not used and therefore support is needed.
      if (resourceAccess instanceof ModelRepositorySnapshot snapshot) {
        return snapshot;
      }
      throw new UnsupportedOperationException();
    }

    @Override
    public CorrespondenceObjectViewObjectTranslator getCorrespondenceResolver() {
      return new PassthroughCorrespondenceObjectViewObjectTranslatorImpl(metamodel);
    }

    @Override
    public void commit() {
      // Because we are working directly on the resource access of the model, there is no need to
      // commit.
    }

    @Override
    public List<EChange<EObject>> fitAndDetermineChanges(ResourceAccess changedOrigin,
                                                         CorrespondenceModelAccess changedCorrespondenceModel,
                                                         List<EChange<EObject>> originChanges) {
      this.resourceAccess = changedOrigin;
      return originChanges;
    }

    @Override
    public void close() {

    }
  }
}
