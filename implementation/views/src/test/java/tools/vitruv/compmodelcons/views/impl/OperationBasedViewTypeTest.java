package tools.vitruv.compmodelcons.views.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
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
import org.mockito.ArgumentCaptor;
import pcm_mockup.Component;
import pcm_mockup.Pcm_mockupPackage;
import pcm_mockup.Repository;
import tools.vitruv.change.atomic.eobject.*;
import tools.vitruv.change.atomic.feature.FeatureEChange;
import tools.vitruv.change.atomic.root.RootEChange;
import tools.vitruv.change.atomic.uuid.Uuid;
import tools.vitruv.change.atomic.uuid.UuidResolver;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.operations.*;
import tools.vitruv.framework.views.ChangeableViewSource;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewType;
import tools.vitruv.framework.views.impl.IdentityMappingViewType;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static tools.vitruv.change.testutils.metamodels.PcmMockupCreators.pcm;

class OperationBasedViewTypeTest {
    @TempDir
    Path tempDir;
    private ChangeableViewSource viewSource;

    @BeforeAll
    static void beforeAll() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
    }

    @BeforeEach
    public void beforeEach() {
        ResourceSet resourceSet = new ResourceSetImpl();
        UuidResolver uuidResolver = UuidResolver.create(resourceSet);

        Resource resource = resourceSet.createResource(URI.createURI(String.format("file://%s/test.xmi", tempDir.toAbsolutePath())));

        Repository repository = pcm.Repository();
        uuidResolver.registerEObject(repository);
        repository.setId("RepositoryID");
        resource.getContents().add(repository);

        Component component1 = pcm.Component();
        uuidResolver.registerEObject(component1);
        component1.setId("ComponentID_1");
        repository.getComponents().add(component1);

        Component component2 = pcm.Component();
        uuidResolver.registerEObject(component2);
        component2.setId("ComponentID_2");
        repository.getComponents().add(component2);

        viewSource = mock(ChangeableViewSource.class);
        when(viewSource.getViewSourceModels()).thenReturn(Set.of(resource));
        when(viewSource.getUuidResolver()).thenReturn(uuidResolver);
    }

    @Test
    public void testAdditionAndRemovalOfComponents() {
        EPackage viewType = DynamicModels.createEPackage();
        EClass viewRootClass = DynamicModels.createEClass(viewType);
        EClass viewNonRootClass = DynamicModels.createEClass(viewType);
        EReference nonRootContainment = DynamicModels.createContainmentEReference(viewRootClass, "containment", viewNonRootClass);

        class TestViewType extends OperationBasedViewType {
            public TestViewType() {
                super("Test", List.of(Pcm_mockupPackage.eINSTANCE), viewType);
            }

            @Override
            protected Root createStructure() {
                return new Root(viewRootClass,
                        Optional.of(
                                new Project(viewRootClass,
                                        new Source(Pcm_mockupPackage.eINSTANCE.getRepository()),
                                        List.of())),
                        List.of(
                                new Root.Target(
                                nonRootContainment,
                                        new Project(
                                                viewNonRootClass,
                                                new Source(Pcm_mockupPackage.eINSTANCE.getComponent()),
                                                List.of()))));
            }
        }

        test(new TestViewType(), view -> {
            Repository repository = view.getRootObjects(Repository.class).iterator().next();
            repository.getComponents().add(pcm.Component());
            repository.getComponents().remove(0);
            repository.getComponents().remove(0);
            repository.getComponents().add(pcm.Component());
            repository.getComponents().add(pcm.Component());
        }, view -> {
            EObject root = view.getRootObjects().iterator().next();
            DynamicModels.getList(root, nonRootContainment).add(DynamicModels.createEObject(viewNonRootClass));
            DynamicModels.getList(root, nonRootContainment).remove(0);
            DynamicModels.getList(root, nonRootContainment).remove(0);
            DynamicModels.getList(root, nonRootContainment).add(DynamicModels.createEObject(viewNonRootClass));
            DynamicModels.getList(root, nonRootContainment).add(DynamicModels.createEObject(viewNonRootClass));
        });
    }

    @Test
    public void testChangeOfComponentId() {
        EPackage viewType = DynamicModels.createEPackage();
        EClass viewRootClass = DynamicModels.createEClass(viewType, "Root");
        EClass viewNonRootClass = DynamicModels.createEClass(viewType, "NonRoot");
        EReference nonRootContainment = DynamicModels.createContainmentEReference(viewRootClass, "containment", viewNonRootClass);
        EAttribute nonRootIdAttribute = DynamicModels.createEAttribute(viewNonRootClass, "id", EcorePackage.eINSTANCE.getEString());

        class TestViewType extends OperationBasedViewType {
            public TestViewType() {
                super("Test", List.of(Pcm_mockupPackage.eINSTANCE), viewType);
            }

            @Override
            protected Root createStructure() {
                return new Root(viewRootClass,
                        Optional.of(
                                new Project(viewRootClass,
                                        new Source(Pcm_mockupPackage.eINSTANCE.getRepository()),
                                        List.of()
                                )),
                        List.of(
                                new Root.Target(
                                        nonRootContainment,
                                        new Project(
                                                viewNonRootClass,
                                                new Source(Pcm_mockupPackage.eINSTANCE.getComponent()),
                                                List.of(
                                                        new FeatureProject(Optional.of(Pcm_mockupPackage.eINSTANCE.getIdentified_Id()), nonRootIdAttribute, new FeatureSource(Pcm_mockupPackage.eINSTANCE.getIdentified_Id()))
                                                )))));
            }
        }

        test(new TestViewType(), view -> {
            Repository repository = view.getRootObjects(Repository.class).iterator().next();
            Component component1 = repository.getComponents().get(0);
            component1.setId("NewID");
        }, view -> {
            EObject root = view.getRootObjects().iterator().next();
            EObject nonRoot1 = DynamicModels.getList(root, nonRootContainment).get(0);
            nonRoot1.eSet(nonRootIdAttribute, "NewID");
        });
    }

    private void test(ViewType<?> testedViewtype, Consumer<View> baselineConsumer, Consumer<View> testConsumer) {
        var baselineChange = run(new IdentityMappingViewType("Baseline"), baselineConsumer);
        var testedChange = run(testedViewtype, testConsumer);

        var baselineChanges = baselineChange.getEChanges();
        var testedChanges = testedChange.getEChanges();

        assertEquals(baselineChanges.size(), testedChanges.size());

        // Order of deletions varies.
        Set<Uuid> baselineDeletions = new HashSet<>();
        Set<Uuid> testedDeletions = new HashSet<>();

        // UUIDs of creations are not equal.
        BiMap<Uuid, Uuid> creationMap = HashBiMap.create();

        BiConsumer<Uuid, Uuid> assertUuidEquality = (Uuid expected, Uuid tested) -> assertTrue(Objects.equals(expected, tested) || Objects.equals(creationMap.get(expected), tested));

        for (int index = 0; index < baselineChanges.size(); index++) {
            var baselineChangeElement = baselineChanges.get(index);
            var testedChangeElement = testedChanges.get(index);

            assertEquals(baselineChangeElement.getClass(), testedChangeElement.getClass());

            if (baselineChangeElement instanceof DeleteEObject<Uuid> baselineDeleteEObject) {
                DeleteEObject<Uuid> testedDeleteEObject = (DeleteEObject<Uuid>) testedChangeElement;
                baselineDeletions.add(baselineDeleteEObject.getAffectedElement());
                testedDeletions.add(testedDeleteEObject.getAffectedElement());

                continue;
            }

            if (baselineChangeElement instanceof CreateEObject<Uuid> baselineCreateEObject) {
                CreateEObject<Uuid> testedCreateEObject = (CreateEObject<Uuid>) testedChangeElement;

                creationMap.put(baselineCreateEObject.getAffectedElement(), testedCreateEObject.getAffectedElement());

                continue;
            }

            if (baselineChangeElement instanceof FeatureEChange<Uuid, ?> baselineFeatureEChange) {
                FeatureEChange<Uuid, ?> testedFeatureEChange = (FeatureEChange<Uuid, ?>) testedChangeElement;
                assertUuidEquality.accept(baselineFeatureEChange.getAffectedElement(), testedFeatureEChange.getAffectedElement());
                assertEquals(baselineFeatureEChange.getAffectedFeature(), testedFeatureEChange.getAffectedFeature());
            }

            if (baselineChangeElement instanceof EObjectExistenceEChange<Uuid> baselineEObjectExistenceEChange) {
                EObjectExistenceEChange<Uuid> testedEObjectExistenceEChange = (EObjectExistenceEChange<Uuid>) testedChangeElement;
                assertUuidEquality.accept(baselineEObjectExistenceEChange.getAffectedElement(), testedEObjectExistenceEChange.getAffectedElement());
            }

            if (baselineChangeElement instanceof EObjectAddedEChange<Uuid> baselineEObjectAddedEChange) {
                EObjectAddedEChange<Uuid> testedEObjectAddedEChange = (EObjectAddedEChange<Uuid>) testedChangeElement;
                assertUuidEquality.accept(baselineEObjectAddedEChange.getNewValue(), testedEObjectAddedEChange.getNewValue());
            }

            if (baselineChangeElement instanceof EObjectSubtractedEChange<Uuid> baselineEObjectSubtractedEChange) {
                EObjectSubtractedEChange<Uuid> testedEObjectSubtractedEChange = (EObjectSubtractedEChange<Uuid>) testedChangeElement;
                assertUuidEquality.accept(baselineEObjectSubtractedEChange.getOldValue(), testedEObjectSubtractedEChange.getOldValue());
            }

            if (baselineChangeElement instanceof RootEChange<Uuid> baselineRootEChange) {
                RootEChange<Uuid> testedRootEChange = (RootEChange<Uuid>) testedChangeElement;
                assertSame(baselineRootEChange.getResource(), testedRootEChange.getResource());
            }
        }

        assertEquals(baselineDeletions, testedDeletions);
    }

    private VitruviusChange<Uuid> run(ViewType<?> viewtype, Consumer<View> consumer) {
        try (View view = createView(viewtype, viewSource)) {
            CommittableView committableView = view.withChangeRecordingTrait();
            consumer.accept(committableView);
            committableView.commitChanges();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        @SuppressWarnings("unchecked") ArgumentCaptor<VitruviusChange<Uuid>> changeArgument = ArgumentCaptor.forClass(VitruviusChange.class);
        verify(viewSource).propagateChange(changeArgument.capture());
        var result = changeArgument.getValue();
        clearInvocations(viewSource);
        return result;
    }

    private View createView(ViewType<?> viewtype, ChangeableViewSource viewSource) {
        var selector = viewtype.createSelector(viewSource);
        selector.getSelectableElements().forEach(eObject -> selector.setSelected(eObject, true));
        return selector.createView();
    }
}