package tools.vitruv.compmodelcons.generator.backend;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.jupiter.api.Test;
import tools.vitruv.compmodelcons.views.DynamicModels;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class ViewTypeSourceGeneratorTest extends AbstractGeneratorTest {
    @Test
    public void testGeneratorShouldFollowNamingConventions() throws URISyntaxException, IOException {
        EPackage viewType = createEPackage();

        EClass root = DynamicModels.createEClass(viewType, "Root");
        EClass restaurant = DynamicModels.createEClass(viewType, "Restaurant");

        DynamicModels.createManyContainmentEReference(root, "allRestaurants", restaurant);

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

        DynamicModels.createManyContainmentEReference(root, "allRestaurants", restaurant);

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

    @Test
    public void testGeneratorShouldGenerateCodeThatCompiles3() throws URISyntaxException, IOException {
        EPackage viewType = createEPackage();

        EClass root = DynamicModels.createEClass(viewType, "Root");
        EClass restaurant = DynamicModels.createEClass(viewType, "Restaurant");

        DynamicModels.createManyContainmentEReference(root, "allRestaurants", restaurant);
        DynamicModels.createEAttribute(restaurant, "numEmployees", EcorePackage.eINSTANCE.getEInt());

        ViewTypeSourceGenerator generator = createGenerator(viewType, "test", """
                from Restaurant r
                create {
                    r.numEmployees
                }
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
                                        EReference RESTAURANT__NUM_EMPLOYEES = null;
                                    }
                                }
                                """));
    }

    @Test
    public void testGeneratorShouldGenerateCodeThatCompiles4() throws URISyntaxException, IOException {
        EPackage viewType = createEPackage();

        EClass root = DynamicModels.createEClass(viewType, "Root");
        EClass joined = DynamicModels.createEClass(viewType, "Joined");

        DynamicModels.createManyContainmentEReference(root, "allJoineds", joined);
        DynamicModels.createEAttribute(joined, "numEmployees", EcorePackage.eINSTANCE.getEInt());
        DynamicModels.createEAttribute(joined, "name", EcorePackage.eINSTANCE.getEString());

        ViewTypeSourceGenerator generator = createGenerator(viewType, "test", """
                from Restaurant r
                join Food f
                create Joined {
                    r.numEmployees
                    f.name
                }
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
                                        EClass JOINED = null;
                                        EReference ROOT__ALL_JOINEDS = null;
                                        EReference JOINED__NUM_EMPLOYEES = null;
                                        EReference JOINED__NAME = null;
                                    }
                                }
                                """));
    }

    @Test
    public void testGeneratorShouldGenerateCodeThatCompiles5() throws URISyntaxException, IOException {
        EPackage viewType = createEPackage();

        EClass root = DynamicModels.createEClass(viewType, "Root");
        EClass joined = DynamicModels.createEClass(viewType, "Joined");

        DynamicModels.createManyContainmentEReference(root, "allJoineds", joined);
        DynamicModels.createEAttribute(joined, "numEmployees", EcorePackage.eINSTANCE.getEInt());
        DynamicModels.createEAttribute(joined, "name", EcorePackage.eINSTANCE.getEString());

        ViewTypeSourceGenerator generator = createGenerator(viewType, "test", """
                from Restaurant r
                join Food f
                where r.numEmployees > 10
                create Joined {
                    r.numEmployees
                    f.name
                }
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
                                        EClass JOINED = null;
                                        EReference ROOT__ALL_JOINEDS = null;
                                        EReference JOINED__NUM_EMPLOYEES = null;
                                        EReference JOINED__NAME = null;
                                    }
                                }
                                """),
                new JavaSourceFromString("neojoin/viewtypes/mymodel/ExpressionStubClass.java",
                        """
                                package neojoin.viewtypes.mymodel;
                                
                                class ExpressionStubClass {
                                    static boolean method(
                                        models.restaurant.Restaurant restaurant,
                                        models.restaurant.Food food
                                    ) {
                                        return true;
                                    }
                                }
                                """));
    }

    @Test
    public void testGeneratorShouldGenerateCodeThatCompiles6() throws URISyntaxException, IOException {
        EPackage viewType = createEPackage();

        EClass root = DynamicModels.createEClass(viewType, "Root");
        EClass joined = DynamicModels.createEClass(viewType, "Joined");

        DynamicModels.createManyContainmentEReference(root, "allJoineds", joined);
        DynamicModels.createEAttribute(joined, "numEmployees", EcorePackage.eINSTANCE.getEInt());
        DynamicModels.createEAttribute(joined, "name", EcorePackage.eINSTANCE.getEString());

        ViewTypeSourceGenerator generator = createGenerator(viewType, "test", """
                from Restaurant r
                join Food f
                    using name
                create Joined {
                    r.numEmployees
                    f.name
                }
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
                                        EClass JOINED = null;
                                        EReference ROOT__ALL_JOINEDS = null;
                                        EReference JOINED__NUM_EMPLOYEES = null;
                                        EReference JOINED__NAME = null;
                                    }
                                }
                                """));
    }

    @Test
    public void testGeneratorShouldGenerateCodeThatCompiles7() throws URISyntaxException, IOException {
        EPackage viewType = createEPackage();

        EClass root = DynamicModels.createEClass(viewType, "Root");
        EClass joined = DynamicModels.createEClass(viewType, "Joined");

        DynamicModels.createManyContainmentEReference(root, "allJoineds", joined);
        DynamicModels.createEAttribute(joined, "numEmployees", EcorePackage.eINSTANCE.getEInt());
        DynamicModels.createEAttribute(joined, "name", EcorePackage.eINSTANCE.getEString());

        ViewTypeSourceGenerator generator = createGenerator(viewType, "test", """
                from Restaurant r
                join Food f
                    on r.name == f.name
                    using name, name, name, name
                create Joined {
                    r.numEmployees
                    f.name
                }
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
                                        EClass JOINED = null;
                                        EReference ROOT__ALL_JOINEDS = null;
                                        EReference JOINED__NUM_EMPLOYEES = null;
                                        EReference JOINED__NAME = null;
                                    }
                                }
                                """),
                new JavaSourceFromString("neojoin/viewtypes/mymodel/ExpressionStubClass.java",
                        """
                                package neojoin.viewtypes.mymodel;
                                
                                class ExpressionStubClass {
                                    static boolean method(
                                        models.restaurant.Restaurant restaurant,
                                        models.restaurant.Food food
                                    ) {
                                        return true;
                                    }
                                }
                                """));
    }

    @Test
    public void testGeneratorShouldGenerateCodeThatCompiles8() throws URISyntaxException,
                                                                      IOException {
        EPackage viewType = createEPackage();

        EClass root = DynamicModels.createEClass(viewType, "Root");
        DynamicModels.createEAttribute(root, "value", EcorePackage.eINSTANCE.getEInt());

        ViewTypeSourceGenerator generator = createGenerator(viewType, "test", """
                                                                              from Restaurant r
                                                                              create root Root {
                                                                                  value := 12 + 47
                                                                              }
                                                                              """
        );

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
                                                 EReference ROOT__VALUE = null;
                                             }
                                         }
                                         """
                ),
                new JavaSourceFromString("neojoin/viewtypes/mymodel/ExpressionStubClass.java",
                                         """
                                         package neojoin.viewtypes.mymodel;
                                         
                                         class ExpressionStubClass {
                                             static boolean method(
                                                 models.restaurant.Restaurant it,
                                                 models.restaurant.Restaurant restaurant
                                             ) {
                                                 return true;
                                             }
                                         }
                                         """
                )
        );
    }
}
