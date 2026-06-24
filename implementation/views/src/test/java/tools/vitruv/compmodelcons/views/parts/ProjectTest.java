package tools.vitruv.compmodelcons.views.parts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

public class ProjectTest extends AbstractPartTest {
    private Models models;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        models = loadModels();
    }

    @Test
    public void test() {
        Project project = new Project();
    }
}
