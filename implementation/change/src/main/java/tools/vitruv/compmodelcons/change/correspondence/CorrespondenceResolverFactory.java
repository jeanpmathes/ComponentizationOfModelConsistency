package tools.vitruv.compmodelcons.change.correspondence;

import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;
import tools.vitruv.framework.views.ViewType;

public interface CorrespondenceResolverFactory {
  CorrespondenceResolver createCorrespondenceResolver(ViewType<?> viewType,
                                                      ViewResourceAccess viewResourceAccess);
}
