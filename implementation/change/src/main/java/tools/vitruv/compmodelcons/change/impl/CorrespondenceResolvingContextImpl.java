package tools.vitruv.compmodelcons.change.impl;

import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.CorrespondenceResolvingContext;

public record CorrespondenceResolvingContextImpl(
        ResourceAccess resourceAccess) implements CorrespondenceResolvingContext {
}
