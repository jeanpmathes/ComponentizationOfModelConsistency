package tools.vitruv.compmodelcons.change;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
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
import tools.vitruv.change.correspondence.CorrespondenceFactory;
import tools.vitruv.change.correspondence.Correspondences;
import tools.vitruv.change.correspondence.model.CorrespondenceModel;
import tools.vitruv.change.correspondence.view.CorrespondenceModelViewFactory;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.change.propagation.ModelRepositorySnapshot;
import tools.vitruv.change.propagation.impl.AbstractChangePropagationSpecification;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.impl.RemoteCorrespondenceTranslatingChangeCorrespondenceSpecificationWrapper;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.operations.FeatureProject;
import tools.vitruv.compmodelcons.views.operations.FeatureSource;
import tools.vitruv.compmodelcons.views.operations.Project;
import tools.vitruv.compmodelcons.views.operations.Root;
import tools.vitruv.compmodelcons.views.operations.Source;

class ViewBasedChangePropagationSpecificationAdapterTest {
  @TempDir Path tempDirectory;

  private ChangePropagatingViewTypeSpecification sourceViewType;
  private ChangePropagatingViewTypeSpecification targetViewType;
  private MetamodelInfo sourceOriginInfo;
  private MetamodelInfo targetOriginInfo;

  @BeforeAll
  static void beforeAll() {
    Resource.Factory.Registry.INSTANCE
        .getExtensionToFactoryMap()
        .put("*", new XMIResourceFactoryImpl());
  }

  private MetamodelInfo createMetamodel(String name) {
    MetamodelInfo info = new MetamodelInfo();
    info.metamodel = DynamicModels.createEPackage();
    info.metamodel.setName(name);
    info.metamodel.setNsPrefix(name);
    info.metamodel.setNsURI("http://" + name);
    info.rootClass = DynamicModels.createEClass(info.metamodel, "Root");
    info.nonRootClass = DynamicModels.createEClass(info.metamodel, "NonRoot");
    info.nameAttribute = DynamicModels.createEAttribute(info.nonRootClass, "name",
                                                        EcorePackage.eINSTANCE.getEString());
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
  void testConstructorWithMismatchingSourceMetamodel() {
    var specification = mock(ChangePropagationSpecification.class);
    when(specification.getSourceMetamodelDescriptor()).thenReturn(
        sourceViewType.getViewTypeMetamodelDescriptor());
    when(specification.getTargetMetamodelDescriptor()).thenReturn(
        targetViewType.getViewTypeMetamodelDescriptor());
    var wrappingStrategy =
        new RemoteCorrespondenceTranslatingChangeCorrespondenceSpecificationWrapper(specification);

    MetamodelDescriptor otherMetamodel = MetamodelDescriptor.of(EcorePackage.eINSTANCE);
    ChangePropagatingViewTypeSpecification otherViewType =
        mock(ChangePropagatingViewTypeSpecification.class);
    when(otherViewType.getViewTypeMetamodelDescriptor()).thenReturn(otherMetamodel);
    when(otherViewType.getOriginMetamodelDescriptor()).thenReturn(otherMetamodel);

    assertThrows(IllegalArgumentException.class,
                 () -> new ViewBasedChangePropagationSpecificationAdapter(otherViewType,
                                                                          otherViewType.getOriginMetamodelDescriptor(),
                                                                          wrappingStrategy,
                                                                          targetViewType,
                                                                          targetViewType.getOriginMetamodelDescriptor()));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testPropagateChanges() throws IOException {
    ViewBasedChangePropagationSpecificationAdapter adapter =
        getViewChangePropagationSpecificationAdapter();

    Path projectPath = tempDirectory.resolve("project");
    Files.createDirectories(projectPath);
    Files.createFile(projectPath.resolve("test_project.marker_vitruv"));

    ResourceSet resourceSet = new ResourceSetImpl();
    setupOrigin(resourceSet, projectPath, "source.xmi", sourceOriginInfo, "oldName");
    EObject targetRoot =
        setupOrigin(resourceSet, projectPath, "target.xmi", targetOriginInfo, "targetOldName");

    ResourceAccess changedOrigin = new TestResourceAccess(resourceSet, tempDirectory);
    TestEditableCorrespondenceModelView<Correspondence> correspondenceModelView =
        mock(TestEditableCorrespondenceModelView.class, withSettings()
            .useConstructor()
            .defaultAnswer(CALLS_REAL_METHODS));
    when(correspondenceModelView.getCorrespondenceModel()).thenCallRealMethod();
    CorrespondenceModel correspondenceModel = correspondenceModelView.getCorrespondenceModel();

    ResourceSet previousResourceSet = new ResourceSetImpl();
    setupOrigin(previousResourceSet, projectPath, "source.xmi", sourceOriginInfo, "oldName");
    setupOrigin(previousResourceSet, projectPath, "target.xmi", targetOriginInfo, "targetOldName");
    ModelRepositorySnapshot previousState =
        spy(new TestModelRepositorySnapshot(previousResourceSet, tempDirectory,
                                            correspondenceModel));

    EObject sourceRoot = resourceSet
        .getResources()
        .getFirst()
        .getContents()
        .getFirst();
    EObject sourceNonRoot = ((List<EObject>) sourceRoot.eGet(sourceOriginInfo.rootClass
                                                                 .getEAllContainments()
                                                                 .getFirst())).getFirst();

    sourceNonRoot.eSet(sourceOriginInfo.nameAttribute, "newName");

    EChange<EObject> change = TypeInferringAtomicEChangeFactory
        .getInstance()
        .createReplaceSingleAttributeChange(sourceNonRoot, sourceOriginInfo.nameAttribute,
                                            "oldName", "newName");

    adapter.propagateChanges(List.of(change), correspondenceModelView, changedOrigin,
                             previousState);

    EObject targetNonRoot = ((List<EObject>) targetRoot.eGet(targetOriginInfo.rootClass
                                                                 .getEAllContainments()
                                                                 .getFirst())).getFirst();
    assertEquals("propagatedValue", targetNonRoot.eGet(targetOriginInfo.nameAttribute));
  }

  private ViewBasedChangePropagationSpecificationAdapter getViewChangePropagationSpecificationAdapter() {
    ChangePropagationSpecification functionalSpecification =
        new TestChangePropagationSpecification(sourceViewType.getViewTypeMetamodelDescriptor(),
                                               targetViewType.getViewTypeMetamodelDescriptor());
    var wrappingStrategy =
        new RemoteCorrespondenceTranslatingChangeCorrespondenceSpecificationWrapper(
            functionalSpecification);
    return new ViewBasedChangePropagationSpecificationAdapter(sourceViewType,
                                                              sourceViewType.getOriginMetamodelDescriptor(),
                                                              wrappingStrategy, targetViewType,
                                                              targetViewType.getOriginMetamodelDescriptor());
  }

  private EObject setupOrigin(ResourceSet resourceSet, Path projectPath, String fileName,
                              MetamodelInfo info, String name) {
    Resource resource = resourceSet.createResource(URI.createFileURI(projectPath
                                                                         .resolve(fileName)
                                                                         .toString()));
    EObject root = DynamicModels.createEObject(info.rootClass);
    resource
        .getContents()
        .add(root);
    EObject nonRoot = DynamicModels.createEObject(info.nonRootClass);
    nonRoot.eSet(info.nameAttribute, name);
    //noinspection unchecked
    ((List<EObject>) root.eGet(info.rootClass
                                   .getEAllContainments()
                                   .getFirst())).add(nonRoot);
    return root;
  }

  private static class MetamodelInfo {
    EPackage metamodel;
    EClass rootClass;
    EClass nonRootClass;
    EAttribute nameAttribute;
  }

  private abstract static class TestEditableCorrespondenceModelView<T extends Correspondence>
      implements EditableCorrespondenceModelView<T> {
    private final CorrespondenceModel correspondenceModel =
        mock(TestCorrespondenceModel.class, withSettings()
            .useConstructor()
            .defaultAnswer(CALLS_REAL_METHODS));

    public TestEditableCorrespondenceModelView() {
    }

    public CorrespondenceModel getCorrespondenceModel() {
      return correspondenceModel;
    }
  }

  private abstract static class TestCorrespondenceModel implements CorrespondenceModel {
    private final Correspondences correspondences =
        CorrespondenceFactory.eINSTANCE.createCorrespondences();

    public TestCorrespondenceModel() {
      ResourceSet resourceSet = new ResourceSetImpl();
      Resource resource = new ResourceImpl(URI.createURI("test_correspondence_model.xmi"));

      resourceSet
          .getResources()
          .add(resource);
      resource
          .getContents()
          .add(correspondences);
    }
  }

  private static class TestChangePropagationSpecification
      extends AbstractChangePropagationSpecification {
    public TestChangePropagationSpecification(MetamodelDescriptor source,
                                              MetamodelDescriptor target) {
      super(source, target);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void propagateChanges(List<EChange<EObject>> changes,
                                 EditableCorrespondenceModelView<Correspondence> correspondenceModel,
                                 ResourceAccess resourceAccess,
                                 ModelRepositorySnapshot previousState) {
      for (EChange<EObject> change : changes) {
        if (change instanceof ReplaceSingleValuedEAttribute) {
          EObject targetRoot = resourceAccess
              .getModelResources()
              .iterator()
              .next()
              .getContents()
              .getFirst();
          List<EObject> targetNonRoots = (List<EObject>) targetRoot.eGet(targetRoot
                                                                             .eClass()
                                                                             .getEStructuralFeature(
                                                                                 "nonRoots"));
          for (EObject targetNonRoot : targetNonRoots) {
            targetNonRoot.eSet(targetNonRoot
                                   .eClass()
                                   .getEStructuralFeature("name"), "propagatedValue");
          }
        }
      }
    }

    @Override
    public boolean doesHandleChange(EChange<EObject> change,
                                    EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
      return true;
    }

    @Override
    public void propagateChange(EChange<EObject> eChange,
                                EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView,
                                ResourceAccess resourceAccess) {
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
      return new Root(viewInfo.rootClass, Optional.of(
          new Project(viewInfo.rootClass, new Source(originInfo.rootClass, null), List.of(),
                      Project.OnPut.NO_OP)), List.of(new Root.Target(viewInfo.rootClass
                                                                         .getEAllContainments()
                                                                         .getFirst(), new Project(
          viewInfo.nonRootClass, new Source(originInfo.nonRootClass, null), List.of(
          new FeatureProject(Optional.of(0), viewInfo.nameAttribute, new FeatureSource(
              FeatureSource.Target.ofFirst(originInfo.nameAttribute)))), Project.OnPut.NO_OP))));
    }
  }

  private static class TestResourceAccess implements ResourceAccess {
    protected final ResourceSet resourceSet;
    private final Path tempDirectory;

    public TestResourceAccess(ResourceSet resourceSet, Path tempDirectory) {
      this.resourceSet = resourceSet;
      this.tempDirectory = tempDirectory;
    }

    @Override
    public URI getMetadataModelURI(String... strings) {
      Path path = tempDirectory;
      for (String s : strings) {
        path = path.resolve(s);
      }
      return URI.createFileURI(path.toString());
    }

    @Override
    public Resource getModelResource(URI uri) {
      Resource resource = resourceSet.getResource(uri, false);
      if (resource == null) {
        resource = resourceSet.createResource(uri);
      }
      return resource;
    }

    @Override
    public Collection<Resource> getModelResources() {
      return new ArrayList<>(resourceSet.getResources());
    }

    @Override
    public void persistAsRoot(EObject eObject, URI uri) {
      getModelResource(uri)
          .getContents()
          .add(eObject);
    }
  }

  private static class TestModelRepositorySnapshot extends TestResourceAccess
      implements ModelRepositorySnapshot {
    private final CorrespondenceModel correspondenceModel;

    public TestModelRepositorySnapshot(ResourceSet resourceSet, Path tempDirectory,
                                       CorrespondenceModel correspondenceModel) {
      super(resourceSet, tempDirectory);
      this.correspondenceModel = correspondenceModel;
    }

    @Override
    public EditableCorrespondenceModelView<Correspondence> getCorrespondenceModel() {
      return CorrespondenceModelViewFactory.createEditableCorrespondenceModelView(
          correspondenceModel);
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
    public void close() {
    }
  }
}
