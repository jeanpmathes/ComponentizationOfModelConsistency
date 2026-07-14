package tools.vitruv.compmodelcons.change.impl;

import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.ChangePropagationSpecification;
import tools.vitruv.compmodelcons.change.AbstractChangePropagationSpecificationWrappingStrategy;

public class RemoteChangePropagationSpecificationWrappingStrategy extends AbstractChangePropagationSpecificationWrappingStrategy {
    public RemoteChangePropagationSpecificationWrappingStrategy(ChangePropagationSpecification specification) {
        super(specification);
    }

    @Override
    public EditableCorrespondenceModelView<Correspondence> wrapCorrespondenceModel(EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
        return correspondenceModel;
    }
}
