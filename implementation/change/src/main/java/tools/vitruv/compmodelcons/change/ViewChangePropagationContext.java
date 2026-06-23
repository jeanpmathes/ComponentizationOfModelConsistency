package tools.vitruv.compmodelcons.change;

import tools.vitruv.framework.views.View;

public record ViewChangePropagationContext(View sourceView,
                                           ViewChangePropagationSpecification sourceViewType,
                                           View targetView,
                                           ViewChangePropagationSpecification targetViewType) {
}
