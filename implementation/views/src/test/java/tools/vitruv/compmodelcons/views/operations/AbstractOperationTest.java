package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.compmodelcons.views.Context;
import tools.vitruv.compmodelcons.views.EditableViewCorrespondences;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.impl.EditableViewCorrespondencesImpl;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class AbstractOperationTest {
    protected Models models;
    protected EditableViewCorrespondences correspondences;

    protected Context context;

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

    protected static EPackage createEPackage() {
        return EcoreFactory.eINSTANCE.createEPackage();
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

    protected static EClass createEClass(EPackage ePackage) {
        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        ePackage.getEClassifiers().add(eClass);
        return eClass;
    }

    protected static <T> void assertForAll(Collection<T> collection, Predicate<? super T> predicate) {
        assertTrue(collection.stream().allMatch(predicate));
    }

    protected static <T> boolean isTrueForOne(Collection<T> collection, Predicate<? super T> predicate) {
        return collection.stream().filter(predicate).count() == 1;
    }

    @BeforeEach
    public void setUp() throws URISyntaxException {
        models = loadModels();
        correspondences = new EditableViewCorrespondencesImpl();

        final Resource viewModel = models.createViewModel();

        context = new Context() {
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
            public Resource getOriginModel(EPackage ePackage) {
                return models.getModel(ePackage);
            }

            @Override
            public Resource getViewModel() {
                return viewModel;
            }

            @Override
            public EditableViewCorrespondences getCorrespondences() {
                return correspondences;
            }
        };
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