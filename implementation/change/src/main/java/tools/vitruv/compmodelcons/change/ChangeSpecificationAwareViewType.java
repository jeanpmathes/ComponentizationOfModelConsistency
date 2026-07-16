package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.views.impl.OperationBasedViewType;
import tools.vitruv.compmodelcons.views.impl.ViewResourceAccessImpl;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;
import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;
import tools.vitruv.compmodelcons.views.internal.impl.InternalViewImpl;
import tools.vitruv.compmodelcons.views.internal.impl.ResourceAccessWrappingOriginResourceAccess;

import java.util.Collection;
import java.util.List;

public abstract class ChangeSpecificationAwareViewType extends OperationBasedViewType implements ChangePropagationViewTypeSpecification {
    public ChangeSpecificationAwareViewType(String name, List<EPackage> originMetamodels, EPackage viewTypeMetamodel) {
        super(name, originMetamodels, viewTypeMetamodel);
    }

    @Override
    public ChangePropagationView createView(ResourceAccess resourceAccess) {
        return new ChangePropagationViewImpl(resourceAccess);
    }

    private class ChangePropagationViewImpl implements ChangePropagationView {
        private final ViewResourceAccess viewResourceAccess;
        private final OriginResourceAccess originResourceAccess;
        private final InternalViewImpl internalView;

        public ChangePropagationViewImpl(ResourceAccess resourceAccess) {
            this.viewResourceAccess = new ViewResourceAccessImpl(getName());
            this.originResourceAccess = new ResourceAccessWrappingOriginResourceAccess(resourceAccess);
            this.internalView = new InternalViewImpl(getStructure(), viewResourceAccess, originResourceAccess);
        }

        @Override
        public void update() {
            internalView.update();
        }

        @Override
        public ResourceAccess getViewResourceAccess() {
            return new ResourceAccess() {
                @Override
                public URI getMetadataModelURI(String... strings) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Resource getModelResource(URI uri) {
                    return viewResourceAccess.getResourceSet().getResource(uri, true);
                }

                @Override
                public Collection<Resource> getModelResources() {
                    return viewResourceAccess.getResourceSet().getResources();
                }

                @Override
                public void persistAsRoot(EObject eObject, URI uri) {
                    viewResourceAccess.registerRoot(eObject, uri);
                }
            };
        }

        @Override
        public List<EChange<EObject>> doGetChange(EChange<EObject> originChange) {
            return internalView.doGetChange(originChange);
        }

        @Override
        public void commit() {
            internalView.commit();
        }

        @Override
        public void close() throws Exception {
            internalView.close();

            viewResourceAccess.close();
            originResourceAccess.close();
        }
    }
}
