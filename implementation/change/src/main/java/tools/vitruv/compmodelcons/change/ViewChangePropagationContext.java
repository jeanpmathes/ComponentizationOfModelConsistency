package tools.vitruv.compmodelcons.change;

import tools.vitruv.change.utils.ResourceAccess;

public record ViewChangePropagationContext(ChangePropagationView sourceView,
                                           ChangePropagatingViewTypeSpecification sourceViewType,
                                           ChangePropagationView targetView,
                                           ChangePropagatingViewTypeSpecification targetViewType) {
    public ResourceAccess getResourceAccess() {
        return targetView.getViewResourceAccess();
    }
}
