package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.vsum.VirtualModel;

import java.util.List;

public interface ViewChangePropagationParticipationSpecification {
    MetamodelDescriptor getOriginMetamodelDescriptor();

    MetamodelDescriptor getViewTypeMetamodelDescriptor();

    View getView(VirtualModel vsum);
}
