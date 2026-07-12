package tools.vitruv.compmodelcons.views.operations;

import com.google.common.collect.Sets;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
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
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueBinding;
import tools.vitruv.compmodelcons.views.impl.EditableViewCorrespondencesImpl;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

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
        if (!context.isAttachmentStateOk()) {
            throw new IllegalStateException("Attachment state is not ok");
        }
    }

    public class TestContext implements GetContext, PutContext {
        private final Resource viewModel = models.createViewModel();

        private final Set<EObject> unattachedCreatedOriginObjects = new HashSet<>();
        private final Set<EObject> undetachedDeletedOriginObjects = new HashSet<>();

        @Override
        public Resource getViewModel() {
            return viewModel;
        }

        @Override
        public void addRootToOriginModel(EPackage originPackage, EObject originObject) {
            models.getModel(originPackage).getContents().add(originObject);
        }

        @Override
        public void removeRootFromOriginModel(EPackage originPackage, EObject originObject) {
            models.getModel(originPackage).getContents().remove(originObject);
        }

        @Override
        public void trackUnattachedCreatedOriginObject(EObject originObject) {
            if (isAttached(originObject)) {
                throw new IllegalArgumentException();
            }

            unattachedCreatedOriginObjects.add(originObject);
            undetachedDeletedOriginObjects.remove(originObject);
        }

        @Override
        public void trackUndetachedDeletedOriginObject(EObject originObject) {
            if (!isAttached(originObject)) {
                throw new IllegalArgumentException();
            }

            undetachedDeletedOriginObjects.add(originObject);
            unattachedCreatedOriginObjects.remove(originObject);
        }

        @Override
        public void trackOriginObjectAttachmentChange(EObject originObject) {
            if (isAttached(originObject)) {
                unattachedCreatedOriginObjects.remove(originObject);
            } else {
                undetachedDeletedOriginObjects.remove(originObject);
            }
        }

        public boolean isAttachmentStateOk() {
            return unattachedCreatedOriginObjects.isEmpty() && undetachedDeletedOriginObjects.isEmpty();
        }

        private boolean isAttached(EObject originObject) {
            return originObject.eResource() != null;
        }

        @Override
        public List<EObject> getOriginObjects(EClass eClass) {
            List<EObject> result = new ArrayList<>();
            for (Model model : Model.values()) {
                var iterator = models.getModel(model).getAllContents();
                while (iterator.hasNext()) {
                    EObject eObject = iterator.next();
                    if (eClass.isSuperTypeOf(eObject.eClass())) {
                        result.add(eObject);
                    }
                }
            }
            return result;
        }

        @Override
        public EditableViewCorrespondences getCorrespondences() {
            return correspondences;
        }
    }

    protected static class Models {
        private final ResourceSet resourceSet;
        private final Resource[] models;

        private int viewCounter = 0;

        public Models(ResourceSet resourceSet, Resource[] models) {
            this.resourceSet = resourceSet;
            this.models = models;
        }

        public EPackage getPackage(Model model) {
            return getRoot(model).eClass().getEPackage();
        }

        public EObject getRoot(Model model) {
            return getModel(model).getContents().get(0);
        }

        public Resource getModel(Model model) {
            return models[model.getIndex()];
        }

        public Resource getModel(EPackage ePackage) {
            for (Model model : Model.values()) {
                if (getPackage(model).equals(ePackage)) {
                    return getModel(model);
                }
            }

            throw new IllegalArgumentException("No model found for " + ePackage);
        }

        public Resource createViewModel() {
            return resourceSet.createResource(URI.createURI(String.format("view_%d.xmi", viewCounter++)));
        }
    }
}