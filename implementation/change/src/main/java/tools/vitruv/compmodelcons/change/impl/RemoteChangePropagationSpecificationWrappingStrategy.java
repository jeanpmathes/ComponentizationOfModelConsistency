package tools.vitruv.compmodelcons.change.impl;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.CorrespondenceModelView;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.compmodelcons.change.AbstractChangePropagationSpecificationWrappingStrategy;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class RemoteChangePropagationSpecificationWrappingStrategy extends AbstractChangePropagationSpecificationWrappingStrategy {
    public RemoteChangePropagationSpecificationWrappingStrategy(ChangePropagationSpecification specification) {
        super(specification);
    }

    @Override
    public EditableCorrespondenceModelView<Correspondence> wrapCorrespondenceModel(EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
        return new RemoteCorrespondenceModelImpl(correspondenceModel);
    }

    private record RemoteCorrespondenceModelImpl(
            EditableCorrespondenceModelView<Correspondence> inner) implements EditableCorrespondenceModelView<Correspondence> {

        @Override
        public Correspondence addCorrespondenceBetween(List<EObject> list, List<EObject> list1, String s) {
            return inner.addCorrespondenceBetween(list, list1, s);
        }

        @Override
        public Set<Correspondence> removeCorrespondencesBetween(List<EObject> list, List<EObject> list1, String s) {
            return inner.removeCorrespondencesBetween(list, list1, s);
        }

        @Override
        public <V extends Correspondence> EditableCorrespondenceModelView<V> getEditableView(Class<V> aClass, Supplier<V> supplier) {
            return inner.getEditableView(aClass, supplier);
        }

        @Override
        public boolean hasCorrespondences(List<EObject> list) {
            return inner.hasCorrespondences(list);
        }

        @Override
        public Set<List<EObject>> getCorrespondingEObjects(List<EObject> list) {
            return inner.getCorrespondingEObjects(list);
        }

        @Override
        public Set<List<EObject>> getCorrespondingEObjects(List<EObject> list, String s) {
            return inner.getCorrespondingEObjects(list, s);
        }

        @Override
        public <V extends Correspondence> CorrespondenceModelView<V> getView(Class<V> aClass) {
            return inner.getView(aClass);
        }
    }
}
