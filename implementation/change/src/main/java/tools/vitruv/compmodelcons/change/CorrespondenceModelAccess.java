package tools.vitruv.compmodelcons.change;

import java.lang.reflect.Field;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.Correspondences;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

/**
 * This class is used as an ugly workaround for the currently missing correspondence handling
 * syntax in the NeoJoin language.
 * When using correspondences, the used workaround is to join in the correspondence model, but
 * the Vitruvius design works against this.
 * Instead of changing the Vitruvius design, this reflection-based access is used to circumvent
 * this problem.
 * Arguments against changing the Vitruvius design are:
 * - It would require a larger change as the separation of correspondence model and other models
 * seems to be deeply integrated.
 * - It is only necessary until the NeoJoin language supports correspondences directly, at which
 * point the correspondence model view is enough.
 * This also means that this class should be removed as soon as the NeoJoin language supports
 * correspondences directly.
 */
public final class CorrespondenceModelAccess implements AutoCloseable {
  private final Correspondences correspondences;
  private final CorrespondenceModelAccess original;

  public CorrespondenceModelAccess(
      EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView) {
    this(extractCorrespondences(editableCorrespondenceModelView), null);
  }

  private CorrespondenceModelAccess(Correspondences correspondences,
                                    CorrespondenceModelAccess original) {
    this.correspondences = correspondences;
    this.original = original;
  }

  private static Correspondences extractCorrespondences(
      EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView) {
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

  public Resource getResource() {
    return correspondences.eResource();
  }

  @Override
  public void close() {
    if (original != null) {
      Resource resource = correspondences.eResource();
      resource.unload();
      resource
          .getResourceSet()
          .getResources()
          .remove(resource);
    }
  }
}
