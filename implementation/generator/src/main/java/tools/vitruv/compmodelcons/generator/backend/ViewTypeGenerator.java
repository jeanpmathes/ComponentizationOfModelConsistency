package tools.vitruv.compmodelcons.generator.backend;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import java.util.Map;
import tools.vitruv.compmodelcons.generator.backend.description.ViewTypeDescription;
import tools.vitruv.dsls.common.JavaImportHelper;

public final class ViewTypeGenerator {
  public static final ViewTypeGenerator INSTANCE = new ViewTypeGenerator();
  private static final TemplateEngine ENGINE = TemplateEngine.createPrecompiled(ContentType.Plain);

  private ViewTypeGenerator() {
  }

  public String generate(ViewTypeDescription description, JavaImportHelper imports) {
    StringOutput output = new StringOutput();
    ENGINE.render("ViewType", Map.of("viewType", description, "imports", imports), output);
    return output.toString();
  }
}
