package tools.vitruv.compmodelcons.generator.backend;

import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewTypeSourceGeneratorTest extends AbstractGeneratorTest {
    @Test
    public void testGeneratorShouldFollowNamingConventions() throws URISyntaxException, IOException {
        EPackage viewType = createEPackage();

        ViewTypeSourceGenerator generator = createGenerator(viewType, "my_example", """
                from Restaurant r
                create {}
                """);

        assertEquals("neojoin/viewtypes/mymodel/MyExampleViewType.java", generator.getFileName());

        String generated = generator.generate();
        assertTrue(generated.contains("package neojoin.viewtypes.mymodel;"));
        assertTrue(generated.contains("public class MyExampleViewType extends ChangeSpecificationAwareViewType"));
    }

    @Test
    public void testGeneratorShouldGenerateCodeThatCompiles() throws URISyntaxException, IOException {
        EPackage viewType = createEPackage();

        ViewTypeSourceGenerator generator = createGenerator(viewType, "test", """
                from Restaurant r
                create {
                
                }
                """);

        compile(generator,
                new JavaSourceFromString("neojoin/viewtypes/mymodel/MymodelPackage.java",
                        """
                                package neojoin.viewtypes.mymodel;
                                
                                import org.eclipse.emf.ecore.EPackage;
                                
                                public interface MymodelPackage extends EPackage {
                                    MymodelPackage eINSTANCE = null;
                                }
                                """));
    }
}
