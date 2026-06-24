package tools.vitruv.compmodelcons.generator.backend;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest extends AbstractGeneratorTest {
    @Test
    public void testNaming() throws URISyntaxException, IOException {
        Generator generator = createGenerator("model_a2model_b", """
                from Restaurant r
                create {}
                """);

        assertEquals("neojoin/viewtypes/package/ModelA2ModelBViewType.java", generator.getFileName());

        String generated = generator.generate();
        assertTrue(generated.contains("package neojoin.viewtypes.package;"));
        assertTrue(generated.contains("public class ModelA2ModelBViewType extends ChangeSpecificationAwareViewType"));
    }
}