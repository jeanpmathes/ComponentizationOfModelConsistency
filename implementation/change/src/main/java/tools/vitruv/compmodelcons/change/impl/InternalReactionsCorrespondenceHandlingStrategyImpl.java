package tools.vitruv.compmodelcons.change.impl;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.CorrespondenceModelView;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.compmodelcons.change.CorrespondenceHandlingStrategy;
import tools.vitruv.dsls.reactions.runtime.correspondence.CorrespondenceFactory;
import tools.vitruv.dsls.reactions.runtime.correspondence.ReactionsCorrespondence;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReactionsChangePropagationSpecification;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class InternalReactionsCorrespondenceHandlingStrategyImpl implements CorrespondenceHandlingStrategy {
    private final AbstractReactionsChangePropagationSpecification specification;

    public InternalReactionsCorrespondenceHandlingStrategyImpl(AbstractReactionsChangePropagationSpecification specification) {
        this.specification = specification;
    }

    @Override
    public EditableCorrespondenceModelView<Correspondence> getLiftedCorrespondenceModel(EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
        var baseView = correspondenceModel.getEditableView(ReactionsCorrespondence.class, CorrespondenceFactory.eINSTANCE::createReactionsCorrespondence);
        // todo: potentially use baseView and ViewChangePropagationParticipationSpecification::getCorrespondingObjects

        return new LiftedCorrespondenceModelView();
    }

    private static class LiftedCorrespondenceModelView implements EditableCorrespondenceModelView<Correspondence> {

        @Override
        public Correspondence addCorrespondenceBetween(List<EObject> list, List<EObject> list1, String s) {
            return null;
        }

        @Override
        public Set<Correspondence> removeCorrespondencesBetween(List<EObject> list, List<EObject> list1, String s) {
            return Set.of();
        }

        @Override
        public <V extends Correspondence> EditableCorrespondenceModelView<V> getEditableView(Class<V> aClass, Supplier<V> supplier) {
            return null;
        }

        @Override
        public boolean hasCorrespondences(List<EObject> list) {
            return false;
        }

        @Override
        public Set<List<EObject>> getCorrespondingEObjects(List<EObject> list) {
            return Set.of();
        }

        @Override
        public Set<List<EObject>> getCorrespondingEObjects(List<EObject> list, String s) {
            return Set.of();
        }

        @Override
        public <V extends Correspondence> CorrespondenceModelView<V> getView(Class<V> aClass) {
            return null;
        }
    }
}
