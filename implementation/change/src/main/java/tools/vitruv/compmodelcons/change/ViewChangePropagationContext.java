package tools.vitruv.compmodelcons.change;

import tools.vitruv.change.utils.ResourceAccess;

public record ViewChangePropagationContext(ChangePropagationView sourceView,
                                           ChangePropagationViewTypeSpecification sourceViewType,
                                           ChangePropagationView targetView,
                                           ChangePropagationViewTypeSpecification targetViewType) {
    public ResourceAccess getResourceAccess() {
        return targetView.getViewResourceAccess();
    }
}
