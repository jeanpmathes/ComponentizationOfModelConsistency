@import gg.jte.support.ForSupport
@import java.util.List
@import org.apache.commons.text.StringEscapeUtils
@import org.eclipse.emf.ecore.EPackage
@import tools.vitruv.compmodelcons.change.ChangeSpecificationAwareViewType
@import tools.vitruv.compmodelcons.generator.backend.description.ViewTypeDescription
@import tools.vitruv.compmodelcons.views.operations.Root
@import tools.vitruv.dsls.common.JavaImportHelper
@import tools.vitruv.framework.views.ViewTypeProvider

@param ViewTypeDescription viewType
@param JavaImportHelper imports 

!{imports.typeRef(ChangeSpecificationAwareViewType.class);}
public class ${viewType.className()} extends ChangeSpecificationAwareViewType {
    public static final String NAME = "${StringEscapeUtils.escapeJava(viewType.name())}";

    !{imports.typeRef(List.class);}
    !{imports.typeRef(EPackage.class);}
    private static final List<EPackage> originMetamodels = List.of(
        @for(var originMetamodel : ForSupport.of(viewType.originMetamodels()))
            ${originMetamodel.get().getFullyQualifiedPackageInterfaceAccessor()}@if(!originMetamodel.isLast()),@endif
        @endfor
    );

    private static final EPackage viewType = ${viewType.viewtypeMetamodel().getFullyQualifiedPackageInterfaceAccessor()};

    !{imports.typeRef(ViewTypeProvider.class);}
    public ${viewType.className()}(ViewTypeProvider viewTypeProvider) {
        super(NAME, originMetamodels, viewType, viewTypeProvider);
    }
    
    !{imports.typeRef(Root.class);}
    @Override
    protected final Root createStructure() {
        return @template.Root(root = viewType.root(), imports = imports);
    }

    @for(var declaration : viewType.declarations())
        @template.Declaration(declaration = declaration, imports = imports)


    @endfor
}
