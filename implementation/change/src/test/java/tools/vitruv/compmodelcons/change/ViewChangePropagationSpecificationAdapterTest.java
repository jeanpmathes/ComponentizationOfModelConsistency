package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.change.atomic.feature.attribute.ReplaceSingleValuedEAttribute;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.change.propagation.ModelSnapshot;
import tools.vitruv.change.propagation.impl.AbstractChangePropagationSpecification;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.impl.RemoteChangePropagationSpecificationWrappingStrategy;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.operations.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ViewChangePropagationSpecificationAdapterTest {
    @TempDir
    Path tempDirectory;

    private ChangePropagationViewTypeSpecification sourceViewType;
    private ChangePropagationViewTypeSpecification targetViewType;
    private MetamodelInfo sourceOriginInfo;
    private MetamodelInfo targetOriginInfo;

    @BeforeAll
    static void beforeAll() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
    }

    private MetamodelInfo createMetamodel(String name) {
        MetamodelInfo info = new MetamodelInfo();
        info.metamodel = DynamicModels.createEPackage();
        info.metamodel.setName(name);
        info.metamodel.setNsPrefix(name);
        info.metamodel.setNsURI("http://" + name);
        info.rootClass = DynamicModels.createEClass(info.metamodel, "Root");
        info.nonRootClass = DynamicModels.createEClass(info.metamodel, "NonRoot");
        info.nameAttribute = DynamicModels.createEAttribute(info.nonRootClass, "name", EcorePackage.eINSTANCE.getEString());
        DynamicModels.createManyContainmentEReference(info.rootClass, "nonRoots", info.nonRootClass);
        return info;
    }

    @BeforeEach
    void setUp() {
        MetamodelInfo sourceViewInfo = createMetamodel("SourceView");
        sourceOriginInfo = createMetamodel("SourceOrigin");
        MetamodelInfo targetViewInfo = createMetamodel("TargetView");
        targetOriginInfo = createMetamodel("TargetOrigin");

        sourceViewType = new TestViewType("SourceViewType", sourceViewInfo, sourceOriginInfo);
        targetViewType = new TestViewType("TargetViewType", targetViewInfo, targetOriginInfo);
    }

    @Test
    void testConstructorWithMatchingMetamodels() {
        var specification = mock(ChangePropagationSpecification.class);
        when(specification.getSourceMetamodelDescriptor()).thenReturn(sourceViewType.getViewTypeMetamodelDescriptor());
        when(specification.getTargetMetamodelDescriptor()).thenReturn(targetViewType.getViewTypeMetamodelDescriptor());
        var wrappingStrategy = new RemoteChangePropagationSpecificationWrappingStrategy(specification);
        new ViewChangePropagationSpecificationAdapter(
                sourceViewType, 0, wrappingStrategy, targetViewType, 0, ChangeDeterminationMode.CHANGE_DERIVATION);
    }

    @Test
    void testConstructorWithMismatchingSourceMetamodel() {
        var specification = mock(ChangePropagationSpecification.class);
        when(specification.getSourceMetamodelDescriptor()).thenReturn(sourceViewType.getViewTypeMetamodelDescriptor());
        when(specification.getTargetMetamodelDescriptor()).thenReturn(targetViewType.getViewTypeMetamodelDescriptor());
        var wrappingStrategy = new RemoteChangePropagationSpecificationWrappingStrategy(specification);

        MetamodelDescriptor otherMetamodel = MetamodelDescriptor.of(EcorePackage.eINSTANCE);
        ChangePropagationViewTypeSpecification otherViewType = mock(ChangePropagationViewTypeSpecification.class);
        when(otherViewType.getViewTypeMetamodelDescriptor()).thenReturn(otherMetamodel);
        when(otherViewType.getOriginMetamodelDescriptors()).thenReturn(List.of(MetamodelDescriptor.of(sourceOriginInfo.metamodel)));

        assertThrows(IllegalArgumentException.class, () -> new ViewChangePropagationSpecificationAdapter(
                otherViewType, 0, wrappingStrategy, targetViewType, 0, ChangeDeterminationMode.CHANGE_DERIVATION));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPropagateChanges() throws IOException {
        var adapter = getViewChangePropagationSpecificationAdapter();

        Path projectPath = tempDirectory.resolve("project");
        Files.createDirectories(projectPath);
        Files.createFile(projectPath.resolve("test_project.marker_vitruv"));

        ResourceSet resourceSet = new ResourceSetImpl();
        setupOrigin(resourceSet, projectPath, "source.xmi", sourceOriginInfo, "oldName");
        EObject targetRoot = setupOrigin(resourceSet, projectPath, "target.xmi", targetOriginInfo, "targetOldName");

        ResourceAccess changedOrigin = new TestResourceAccess(resourceSet);
        EditableCorrespondenceModelView<Correspondence> correspondenceModel = mock(EditableCorrespondenceModelView.class);

        ResourceSet previousResourceSet = new ResourceSetImpl();
        setupOrigin(previousResourceSet, projectPath, "source.xmi", sourceOriginInfo, "oldName");
        setupOrigin(previousResourceSet, projectPath, "target.xmi", targetOriginInfo, "targetOldName");
        ModelSnapshot previousState = mock(ModelSnapshot.class);
        when(previousState.copy()).thenReturn(new TestModelSnapshot(previousResourceSet));

        EObject sourceRoot = resourceSet.getResources().getFirst().getContents().getFirst();
        EObject sourceNonRoot = ((List<EObject>) sourceRoot.eGet(sourceOriginInfo.rootClass.getEAllContainments().getFirst())).getFirst();

        sourceNonRoot.eSet(sourceOriginInfo.nameAttribute, "newName");

        EChange<EObject> change = TypeInferringAtomicEChangeFactory.getInstance().createReplaceSingleAttributeChange(
                sourceNonRoot, sourceOriginInfo.nameAttribute, "oldName", "newName");

        adapter.propagateChanges(List.of(change), correspondenceModel, changedOrigin, previousState);

        EObject targetNonRoot = ((List<EObject>) targetRoot.eGet(targetOriginInfo.rootClass.getEAllContainments().getFirst())).getFirst();
        assertEquals("propagatedValue", targetNonRoot.eGet(targetOriginInfo.nameAttribute));
    }

    private ViewChangePropagationSpecificationAdapter getViewChangePropagationSpecificationAdapter() {
        ChangePropagationSpecification functionalSpecification = new TestChangePropagationSpecification(
                sourceViewType.getViewTypeMetamodelDescriptor(), targetViewType.getViewTypeMetamodelDescriptor());
        var wrappingStrategy = new RemoteChangePropagationSpecificationWrappingStrategy(functionalSpecification);
        return new ViewChangePropagationSpecificationAdapter(
                sourceViewType, 0, wrappingStrategy, targetViewType, 0, ChangeDeterminationMode.CHANGE_DERIVATION);
    }

    private EObject setupOrigin(ResourceSet resourceSet, Path projectPath, String fileName, MetamodelInfo info, String name) {
        Resource resource = resourceSet.createResource(URI.createFileURI(projectPath.resolve(fileName).toString()));
        EObject root = DynamicModels.createEObject(info.rootClass);
        resource.getContents().add(root);
        EObject nonRoot = DynamicModels.createEObject(info.nonRootClass);
        nonRoot.eSet(info.nameAttribute, name);
        //noinspection unchecked
        ((List<EObject>) root.eGet(info.rootClass.getEAllContainments().getFirst())).add(nonRoot);
        return root;
    }

    private static class MetamodelInfo {
        EPackage metamodel;
        EClass rootClass;
        EClass nonRootClass;
        EAttribute nameAttribute;
    }

    private static class TestChangePropagationSpecification extends AbstractChangePropagationSpecification {
        public TestChangePropagationSpecification(MetamodelDescriptor source, MetamodelDescriptor target) {
            super(source, target);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void propagateChanges(List<EChange<EObject>> changes, EditableCorrespondenceModelView<Correspondence> correspondenceModel, ResourceAccess resourceAccess, ModelSnapshot previousState) {
            for (EChange<EObject> change : changes) {
                if (change instanceof ReplaceSingleValuedEAttribute) {
                    EObject targetRoot = resourceAccess.getModelResources().iterator().next().getContents().getFirst();
                    List<EObject> targetNonRoots = (List<EObject>) targetRoot.eGet(targetRoot.eClass().getEStructuralFeature("nonRoots"));
                    for (EObject targetNonRoot : targetNonRoots) {
                        targetNonRoot.eSet(targetNonRoot.eClass().getEStructuralFeature("name"), "propagatedValue");
                    }
                }
            }
        }

        @Override
        public boolean doesHandleChange(EChange<EObject> change, EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
            return true;
        }

        @Override
        public void propagateChange(EChange<EObject> eChange, EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView, ResourceAccess resourceAccess) {
            throw new UnsupportedOperationException();
        }
    }

    private static class TestViewType extends ChangeSpecificationAwareViewType {
        private final MetamodelInfo viewInfo;
        private final MetamodelInfo originInfo;

        public TestViewType(String name, MetamodelInfo viewInfo, MetamodelInfo originInfo) {
            super(name, List.of(originInfo.metamodel), viewInfo.metamodel);
            this.viewInfo = viewInfo;
            this.originInfo = originInfo;
        }

        @Override
        protected Root createStructure() {
            return new Root(viewInfo.rootClass,
                    Optional.of(new Project(viewInfo.rootClass, new Source(originInfo.rootClass), List.of())),
                    List.of(new Root.Target(viewInfo.rootClass.getEAllContainments().getFirst(),
                            new Project(viewInfo.nonRootClass, new Source(originInfo.nonRootClass), List.of(
                                    new FeatureProject(Optional.of(originInfo.nameAttribute), viewInfo.nameAttribute, new FeatureSource(originInfo.nameAttribute))
                            )))));
        }
    }

    private class TestResourceAccess implements ResourceAccess {
        protected final ResourceSet resourceSet;

        public TestResourceAccess(ResourceSet resourceSet) {
            this.resourceSet = resourceSet;
        }

        @Override
        public URI getMetadataModelURI(String... strings) {
            Path path = tempDirectory;
            for (String s : strings) path = path.resolve(s);
            return URI.createFileURI(path.toString());
        }

        @Override
        public Resource getModelResource(URI uri) {
            Resource resource = resourceSet.getResource(uri, false);
            if (resource == null) resource = resourceSet.createResource(uri);
            return resource;
        }

        @Override
        public Collection<Resource> getModelResources() {
            return new ArrayList<>(resourceSet.getResources());
        }

        @Override
        public void persistAsRoot(EObject eObject, URI uri) {
            getModelResource(uri).getContents().add(eObject);
        }
    }

    private class TestModelSnapshot extends TestResourceAccess implements ModelSnapshot {
        public TestModelSnapshot(ResourceSet resourceSet) {
            super(resourceSet);
        }

        @Override
        public ModelSnapshot copy() {
            return this;
        }

        @Override
        public Optional<EObject> getSnapshotEObject(EObject eObject) {
            return Optional.empty();
        }

        @Override
        public Optional<EObject> getRepositoryEObject(EObject eObject) {
            return Optional.empty();
        }

        @Override
        public void registerEObjectMapping(EObject eObject, EObject eObject1) {
        }

        @Override
        public void close() {
        }
    }
}
