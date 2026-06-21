package tools.vitruv.compmodelcons.change;

import tools.vitruv.framework.views.View;

public record ViewChangePropagationContext(View sourceView,
                                           ViewChangePropagationParticipationSpecification sourceViewType,
                                           View targetView,
                                           ViewChangePropagationParticipationSpecification targetViewType) {
}
