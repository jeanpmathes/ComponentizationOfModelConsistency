package tools.vitruv.compmodelcons.views.parts;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

class AbstractPartTest {
    protected static Models loadModels() throws URISyntaxException {
        ResourceSet resourceSet = new ResourceSetImpl();

        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());

        EPackage[] packages = new EPackage[Model.values().length];
        int packageIndex = 0;

        for (Model model : Model.values()) {
            Resource resource = resourceSet.getResource(model.getMetamodelURI(), true);

            for (EObject content : resource.getContents()) {
                if (content instanceof EPackage ePackage) {
                    resourceSet.getPackageRegistry().put(ePackage.getNsURI(), ePackage);
                    packages[packageIndex++] = ePackage;
                }
            }
        }

        EObject[] roots = new EObject[Model.values().length];
        int rootIndex = 0;

        for (Model model : Model.values()) {
            Resource resource = resourceSet.getResource(model.getModelURI(), true);

            roots[rootIndex++] = resource.getContents().get(0);
        }

        return new Models(packages, roots);
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

    protected record Models(EPackage[] packages, EObject[] roots) {
        public EPackage getPackage(Model model) {
            return packages[model.getIndex()];
        }

        public EObject getRoot(Model model) {
            return roots[model.getIndex()];
        }
    }
}