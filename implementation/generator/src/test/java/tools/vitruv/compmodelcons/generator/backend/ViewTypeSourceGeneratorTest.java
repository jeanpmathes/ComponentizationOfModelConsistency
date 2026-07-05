package tools.vitruv.compmodelcons.generator.backend;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import tools.vitruv.compmodelcons.views.DynamicModels;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewTypeSourceGeneratorTest extends AbstractGeneratorTest {
    @Test
    public void testGeneratorShouldFollowNamingConventions() throws URISyntaxException, IOException {
        EPackage viewType = createEPackage();

        EClass root = DynamicModels.createEClass(viewType, "Root");
        EClass restaurant = DynamicModels.createEClass(viewType, "Restaurant");

        DynamicModels.createContainmentEReference(root, "allRestaurants", restaurant);

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
    public void testGeneratorShouldGenerateCodeThatCompiles1() throws URISyntaxException, IOException {
        EPackage viewType = createEPackage();

        EClass root = DynamicModels.createEClass(viewType, "Root");
        EClass restaurant = DynamicModels.createEClass(viewType, "Restaurant");

        DynamicModels.createContainmentEReference(root, "allRestaurants", restaurant);

        ViewTypeSourceGenerator generator = createGenerator(viewType, "test", """
                from Restaurant r
                create {}
                """);

        compile(generator,
                new JavaSourceFromString("neojoin/viewtypes/mymodel/MymodelPackage.java",
                        """
                                package neojoin.viewtypes.mymodel;
                                
                                import org.eclipse.emf.ecore.EPackage;
                                import org.eclipse.emf.ecore.EClass;
                                import org.eclipse.emf.ecore.EReference;
                                
                                public interface MymodelPackage extends EPackage {
                                    MymodelPackage eINSTANCE = null;
                                
                                    public interface Literals {
                                        EClass ROOT = null;
                                        EClass RESTAURANT = null;
                                        EReference ROOT__ALL_RESTAURANTS = null;
                                    }
                                }
                                """));
    }

    @Test
    public void testGeneratorShouldGenerateCodeThatCompiles2() throws URISyntaxException, IOException {
        EPackage viewType = createEPackage();

        DynamicModels.createEClass(viewType, "Store");

        ViewTypeSourceGenerator generator = createGenerator(viewType, "test", """
                from Restaurant r
                create root Store {}
                """);

        compile(generator,
                new JavaSourceFromString("neojoin/viewtypes/mymodel/MymodelPackage.java",
                        """
                                package neojoin.viewtypes.mymodel;
                                
                                import org.eclipse.emf.ecore.EPackage;
                                import org.eclipse.emf.ecore.EClass;
                                import org.eclipse.emf.ecore.EReference;
                                
                                public interface MymodelPackage extends EPackage {
                                    MymodelPackage eINSTANCE = null;
                                
                                    public interface Literals {
                                        EClass STORE = null;
                                    }
                                }
                                """));
    }
}
