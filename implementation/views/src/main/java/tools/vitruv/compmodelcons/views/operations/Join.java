package tools.vitruv.compmodelcons.views.operations;

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

import java.util.ArrayList;
import java.util.List;

public class Join implements OriginOperation {
    private final EClass sourceClass;
    private final boolean isRoot;
    private final EReference container;
    private final OriginOperation origin;

    public Join(EClass sourceClass, OriginOperation origin) {
        this.sourceClass = sourceClass;
        this.isRoot = DynamicModels.isRoot(sourceClass);
        this.container = isRoot ? null : DynamicModels.getUnambiguousContainer(sourceClass);
        this.origin = origin;
    }

    @Override
    public List<OriginBinding> doGet(GetContext context) {
        return origin.doGet(context).stream()
                .flatMap(originBinding -> context.getOriginObjects(sourceClass).stream()
                        .map(joinedBinding -> (OriginBinding) new JoinOriginBindingImpl(originBinding, joinedBinding)))
                .toList();
    }

    @Override
    public OriginBinding doPut(EChange<EObject> viewChange, OriginBinding target, PutContext context) {
        if (viewChange instanceof CreateEObject<EObject> createEObject) {
            assert target.originObjects().isEmpty();

            OriginBinding originBinding = origin.doPut(viewChange, target, context);

            EObject created = sourceClass.getEPackage().getEFactoryInstance().create(sourceClass);
            context.getCorrespondences().joinCorrespondence(originBinding.originObjects(), List.of(created), createEObject.getAffectedElement());
            Source.attachedCreatedOriginObject(created, sourceClass, isRoot, container, context);

            return new JoinOriginBindingImpl(originBinding, created);
        }

        if (viewChange instanceof DeleteEObject<EObject> deleteEObject) {
            JoinOriginBindingImpl binding = (JoinOriginBindingImpl) target;

            EObject deleted = binding.originObject();
            context.getCorrespondences().unjoinCorrespondence(binding.originObjects(), List.of(deleted), deleteEObject.getAffectedElement());
            Source.detachDeletedOriginObject(deleted, sourceClass, isRoot, container, context);

            origin.doPut(viewChange, binding.originBinding(), context);

            return OriginBinding.empty();
        }

        if (viewChange instanceof InsertRootEObject<EObject> insertRootEObject) {
            JoinOriginBindingImpl binding = (JoinOriginBindingImpl) target;
            EObject inserted = binding.originObject();

            OriginBinding originBinding = origin.doPut(viewChange, binding.originBinding(), context);

            if (isRoot) {
                context.moveRootToOtherOriginModel(sourceClass.getEPackage(), inserted, insertRootEObject.getResource().getURI());
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
    public List<OriginBinding> doUpdatingGet(List<OriginBinding> previous, EChange<EObject> originChange, GetContext context) {
        return List.of();
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
