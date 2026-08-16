package tools.vitruv.compmodelcons.views.operations;

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.CreateEObject;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.atomic.root.RemoveRootEObject;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.OriginBinding;
import tools.vitruv.compmodelcons.views.conditions.Condition;

public class Join implements OriginOperation {
  private final EClass sourceClass;
  private final SourceObjectFactory sourceObjectFactory;
  private final boolean isRoot;
  private final EReference container;
  private final OriginOperation origin;
  private final Type type;
  private final Condition condition;

  public Join(EClass sourceClass, SourceObjectFactory sourceObjectFactory, OriginOperation origin,
              Type type, Condition condition) {
    this.sourceClass = sourceClass;
    this.sourceObjectFactory =
        SourceObjectFactory.requireNonNullElseDefault(sourceObjectFactory, sourceClass);
    this.isRoot = DynamicModels.isRoot(sourceClass);
    this.container = isRoot ? null : DynamicModels.getUnambiguousContainer(sourceClass);
    this.origin = origin;
    this.type = type;
    this.condition = condition;
  }

  @Override
  public List<OriginBinding> doGet(GetContext context) {
    return origin
        .doGet(context)
        .stream()
        .flatMap(originBinding -> {
          Stream<OriginBinding> result = context
              .getOriginObjects(sourceClass)
              .stream()
              .map(joined -> (OriginBinding) new JoinOriginBindingImpl(originBinding, joined))
              .filter(condition::evaluate);

          return type == Type.INNER ? result : defaultIfEmpty(result, () -> originBinding);
        })
        .toList();
  }

  private Stream<OriginBinding> defaultIfEmpty(Stream<OriginBinding> stream,
                                               Supplier<OriginBinding> defaultFunction) {
    Iterator<OriginBinding> iterator = stream.iterator();
    return iterator.hasNext() ? Streams.stream(iterator) : Stream.of(defaultFunction.get());
  }

  @Override
  public OriginBinding doPut(EChange<EObject> viewChange, OriginBinding target,
                             PutContext context) {
    if (viewChange instanceof CreateEObject<EObject> createEObject) {
      assert target
          .originObjects()
          .isEmpty();

      OriginBinding originBinding = origin.doPut(viewChange, target, context);

      // In the case of a self-join (from X join X) always creating is not actually correct.
      // Instead, we would need to check whether the view object already has an origin object in
      // the correspondences
      // that would satisfy this join as well.

      EObject created = sourceObjectFactory.createOriginObject(createEObject.getAffectedElement());
      context
          .getCorrespondences()
          .joinCorrespondence(originBinding.originObjects(), List.of(created),
                              createEObject.getAffectedElement());
      Source.attachedCreatedOriginObject(created, sourceClass, isRoot, container, context);

      return new JoinOriginBindingImpl(originBinding, created);
    }

    if (viewChange instanceof DeleteEObject<EObject> deleteEObject) {
      JoinOriginBindingImpl binding = (JoinOriginBindingImpl) target;

      EObject deleted = binding.originObject();
      context
          .getCorrespondences()
          .unjoinCorrespondence(binding.originObjects(), List.of(deleted),
                                deleteEObject.getAffectedElement());
      Source.detachDeletedOriginObject(deleted, sourceClass, isRoot, container, context);

      origin.doPut(viewChange, binding.originBinding(), context);

      return OriginBinding.empty();
    }

    if (viewChange instanceof InsertRootEObject<EObject> insertRootEObject) {
      JoinOriginBindingImpl binding = (JoinOriginBindingImpl) target;
      EObject inserted = binding.originObject();

      OriginBinding originBinding = origin.doPut(viewChange, binding.originBinding(), context);

      if (isRoot) {
        context.moveRootToOtherOriginModel(sourceClass.getEPackage(), inserted,
                                           insertRootEObject
                                               .getResource()
                                               .getURI());
      }

      return new JoinOriginBindingImpl(originBinding, inserted);
    }

    if (viewChange instanceof RemoveRootEObject<EObject>) {
      JoinOriginBindingImpl binding = (JoinOriginBindingImpl) target;
      EObject removed = binding.originObject();

      OriginBinding originBinding = origin.doPut(viewChange, binding.originBinding(), context);

      if (isRoot) {
        context.moveRootToDefaultOriginModel(sourceClass.getEPackage(), removed);
      }

      return new JoinOriginBindingImpl(originBinding, removed);
    }

    throw new IllegalArgumentException("Inappropriate change type: " + viewChange.getClass());
  }

  @Override
  public List<OriginBinding> doUpdatingGet(List<OriginBinding> previous,
                                           EChange<EObject> originChange, GetContext context) {
    return List.of();
  }

  public enum Type {
    INNER,
    LEFT
  }

  private static final class JoinOriginBindingImpl implements OriginBinding {
    private final OriginBinding originBinding;
    private final EObject originObject;
    private final List<EObject> originObjects = new ArrayList<>();

    private JoinOriginBindingImpl(OriginBinding originBinding, EObject originObject) {
      this.originBinding = originBinding;
      this.originObject = originObject;

      this.originObjects.addAll(originBinding.originObjects());
      this.originObjects.add(originObject);
    }

    @Override
    public List<EObject> originObjects() {
      return originObjects;
    }

    public OriginBinding originBinding() {
      return originBinding;
    }

    public EObject originObject() {
      return originObject;
    }
  }
}
