package tools.vitruv.compmodelcons.views.operations;

import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import tools.vitruv.compmodelcons.views.EditableViewCorrespondences;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueBinding;
import tools.vitruv.compmodelcons.views.impl.EditableViewCorrespondencesImpl;
import tools.vitruv.compmodelcons.views.impl.PutContextImpl;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;
import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractOperationTest {
    protected Models models;
    protected EditableViewCorrespondences correspondences;
    protected TestContext context;

    protected static Models loadModels() throws URISyntaxException {
        ResourceSet resourceSet = new ResourceSetImpl();

        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());

        Resource[] models = new Resource[Model.values().length];
        int modelIndex = 0;

        for (Model model : Model.values()) {
            Resource metamodel = resourceSet.getResource(model.getMetamodelURI(), true);

            for (EObject content : metamodel.getContents()) {
                if (content instanceof EPackage ePackage) {
                    resourceSet.getPackageRegistry().put(ePackage.getNsURI(), ePackage);
                }
            }

            models[modelIndex++] = resourceSet.getResource(model.getModelURI(), true);
        }

        return new Models(resourceSet, models);
    }

    public enum Model {
        RESTAURANT("restaurant", "restaurants"),
        REVIEWPAGE("reviewpage", "reviews");

        private final String metamodel;
        private final String model;

        Model(String metamodel, String model) {
            this.metamodel = metamodel;
            this.model = model;
        }

        public URI getMetamodelURI() throws URISyntaxException {
            return URI.createFileURI(Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(String.format("models/%s.ecore", metamodel))).toURI()).toString());
        }

        public URI getModelURI() throws URISyntaxException {
            return URI.createFileURI(Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(String.format("models/%s.xmi", model))).toURI()).toString());
        }

        public int getIndex() {
            return ordinal();
        }
    }

    protected static ObjectBinding createBinding(EObject originObject, EObject viewObject) {
        return new ObjectBinding() {
            @Override
            public List<EObject> originObjects() {
                return List.of(originObject);
            }

            @Override
            public EObject viewObject() {
                return viewObject;
            }
        };
    }

    protected static FeatureBinding createBinding(EObject originSubjectObject, EObject viewSubjectObject, ValueBinding value) {
        return new FeatureBinding() {
            @Override
            public List<EObject> originSubjectObjects() {
                return List.of(originSubjectObject);
            }

            @Override
            public EObject viewSubjectObject() {
                return viewSubjectObject;
            }

            @Override
            public ValueBinding value() {
                return value;
            }
        };
    }

    protected static <T> void assertForAll(Collection<T> collection, Predicate<? super T> predicate) {
        assertTrue(collection.stream().allMatch(predicate));
    }

    protected static <T> T assertOneAdded(Collection<T> original, Collection<T> modified) {
        Collection<T> difference = Sets.difference(new HashSet<>(modified), new HashSet<>(original));
        assertEquals(1, difference.size());
        return difference.iterator().next();
    }

    @BeforeEach
    public void setUp() throws URISyntaxException {
        models = loadModels();
        correspondences = new EditableViewCorrespondencesImpl();
        context = new TestContext();
    }

    @AfterEach
    public void tearDown() {
        context.validateAttachmentState();
    }

    protected static class Models {
        private final ResourceSet resourceSet;
        private final Resource[] originModels;
        private final EPackage[] originPackages;
        private final List<Resource> viewModels;

        public Models(ResourceSet resourceSet, Resource[] originModels) {
            this.resourceSet = resourceSet;
            this.originModels = originModels;
            this.originPackages = Arrays.stream(originModels).map(resource -> resource.getContents().getFirst().eClass().getEPackage()).toArray(EPackage[]::new);
            this.viewModels = new ArrayList<>();

            viewModels.add(resourceSet.createResource(URI.createURI("view.xmi")));
        }

        public EPackage getPackage(Model model) {
            return originPackages[model.getIndex()];
        }

        public EObject getRoot(Model model) {
            return getModel(model).getContents().getFirst();
        }

        public Resource getModel(Model model) {
            return originModels[model.getIndex()];
        }

        public Collection<Resource> getOtherModels(Model model) {
            return resourceSet.getResources().stream()
                    .filter(resource -> !resource.getContents().isEmpty() && resource.getContents().getFirst().eClass().getEPackage().equals(getPackage(model)))
                    .filter(resource -> resource != getModel(model))
                    .toList();
        }

        public Model getModelIdentifier(EPackage ePackage) {
            for (Model model : Model.values()) {
                if (getPackage(model).equals(ePackage)) {
                    return model;
                }
            }

            throw new IllegalArgumentException("No model found for " + ePackage);
        }

        public Resource getViewModel() {
            return viewModels.getFirst();
        }

        public Resource createViewModel(URI uri) {
            Resource resource = resourceSet.createResource(uri);
            viewModels.add(resource);
            return resource;
        }

        public List<Resource> getViewModels() {
            return viewModels;
        }

        public Resource createOriginModel(EPackage ePackage, URI uri) {
            return resourceSet.createResource(uri.appendFileExtension(".origin").appendFileExtension(ePackage.getNsPrefix()));
        }
    }

    public class TestContext extends PutContextImpl {
        protected TestContext() {
            super(new OriginResourceAccess() {
                @Override
                public Optional<Resource> getDefaultResource(EPackage ePackage) {
                    return Optional.of(models.getModel(models.getModelIdentifier(ePackage)));
                }

                @Override
                public void createResourceWithRoot(URI uriHint, EObject root) {
                    models.createOriginModel(root.eClass().getEPackage(), uriHint).getContents().add(root);
                }

                @Override
                public Collection<Resource> getResources(EPackage ePackage) {
                    Model model = models.getModelIdentifier(ePackage);
                    return Streams.concat(Stream.of(models.getModel(model)), models.getOtherModels(model).stream()).toList();
                }

                @Override
                public Optional<URI> getViewUriHint(EPackage originPackage, EPackage viewtypePackage) {
                    return Optional.empty();
                }

                @Override
                public void refreshResourceMapping() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void close() {

                }
            }, new ViewResourceAccess() {
                @Override
                public void reset() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public ResourceSet getResourceSet() {
                    return models.resourceSet;
                }

                @Override
                public void insertRoot(EObject root) {
                    models.getViewModel().getContents().add(root);
                }

                @Override
                public void registerRoot(EObject root, URI uri) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void moveRoot(EObject root, URI uri) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Collection<EObject> getRoots() {
                    return models.getViewModels().stream().flatMap(resource -> resource.getContents().stream()).toList();
                }

                @Override
                public void close() throws Exception {

                }
            }, correspondences);
        }
    }
}