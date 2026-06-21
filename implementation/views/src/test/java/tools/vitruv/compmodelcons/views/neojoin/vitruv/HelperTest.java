package tools.vitruv.compmodelcons.views.neojoin.vitruv;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.collector.PackageModelCollector;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HelperTest {
    @Test
    void testGetAQR() throws URISyntaxException, IOException {
        PackageModelCollector collector = new PackageModelCollector(Paths.get(HelperTest.class.getClassLoader().getResource("").toURI()).toString());

        Optional<AQR> result = Helper.getAQR(new File(Objects.requireNonNull(getClass().getResource("/movies.nj")).toURI()).getAbsolutePath(), collector.collect());
        assertTrue(result.isPresent());

        AQR aqr = result.get();
    }
}