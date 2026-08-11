package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.Correspondences;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.ModelSnapshot;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class is used as an ugly workaround for the currently missing correspondence handling syntax in the NeoJoin language.
 * When using correspondences, the used workaround is to join in the correspondence model, but the Vitruvius design works against this.
 * Instead of changing the Vitruvius design, this reflection-based access is used to circumvent this problem.
 * Arguments against changing the Vitruvius design are:
 * - It would require a larger change as the separation of correspondence model and other models seems to be deeply integrated.
 * - It is only necessary until the NeoJoin language supports correspondences directly, at which point the correspondence model view is enough.
 * This also means that this class should be removed as soon as the NeoJoin language supports correspondences directly.
 */
public final class CorrespondenceModelAccess implements AutoCloseable {
    private final Correspondences correspondences;
    private final CorrespondenceModelAccess original;

    public CorrespondenceModelAccess(EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView) {
        this(extractCorrespondences(editableCorrespondenceModelView), null);
    }

    private CorrespondenceModelAccess(Correspondences correspondences, CorrespondenceModelAccess original) {
        this.correspondences = correspondences;
        this.original = original;
    }

    private static Correspondences extractCorrespondences(EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView) {
        return getField(getField(editableCorrespondenceModelView, "correspondenceModel", Object.class),
                        "correspondences", Correspondences.class);
    }

    private static <T> T getField(Object object, String name, Class<T> type) {
        Class<?> currentClass = object.getClass();

        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(name);
                field.setAccessible(true);
                return type.cast(field.get(object));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                currentClass = currentClass.getSuperclass();
            }
        }

        throw new IllegalArgumentException("Field not found: " + name);
    }

    private static Optional<List<EObject>> mapToSnapshot(List<EObject> repositoryObjects, ModelSnapshot snapshot) {
        List<EObject> snapshotEObjects = new ArrayList<>(repositoryObjects.size());

        for (EObject repositoryObject : repositoryObjects) {
            Optional<EObject> snapshotEObject = snapshot.getSnapshotEObject(repositoryObject);
            if (snapshotEObject.isEmpty()) {
                return Optional.empty();
            }
            snapshotEObjects.add(snapshotEObject.get());
        }

        return Optional.of(snapshotEObjects);
    }

    public Resource getResource() {
        return correspondences.eResource();
    }

    private CorrespondenceModelAccess getOriginal() {
        return original == null ? this : original;
    }

    public CorrespondenceModelAccess copy(ModelSnapshot modelSnapshot) {
        Correspondences copy = EcoreUtil.copy(correspondences);

        for (int index = correspondences.getCorrespondences().size() - 1; index >= 0; index--) {
            Correspondence originalCorrespondence = correspondences.getCorrespondences().get(index);
            Correspondence copiedCorrespondence = copy.getCorrespondences().get(index);

            Optional<List<EObject>> leftEObjects =
                    mapToSnapshot(originalCorrespondence.getLeftEObjects(), modelSnapshot);
            Optional<List<EObject>> rightEObjects =
                    mapToSnapshot(originalCorrespondence.getRightEObjects(), modelSnapshot);

            if (leftEObjects.isEmpty() || rightEObjects.isEmpty()) {
                copy.getCorrespondences().remove(index);
                continue;
            }

            copiedCorrespondence.getLeftEObjects().clear();
            copiedCorrespondence.getLeftEObjects().addAll(leftEObjects.get());

            copiedCorrespondence.getRightEObjects().clear();
            copiedCorrespondence.getRightEObjects().addAll(rightEObjects.get());
        }

        ResourceSet resourceSet = new ResourceSetImpl();
        Resource resource = resourceSet.createResource(correspondences.eResource().getURI());

        resource.getContents().add(copy);
        resourceSet.getResources().add(resource);

        return new CorrespondenceModelAccess(copy, getOriginal());
    }

    @Override
    public void close() {
        if (original != null) {
            Resource resource = correspondences.eResource();
            resource.unload();
            resource.getResourceSet().getResources().remove(resource);
        }
    }
}
