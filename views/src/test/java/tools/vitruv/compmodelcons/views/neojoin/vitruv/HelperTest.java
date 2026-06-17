package tools.vitruv.compmodelcons.views.neojoin.vitruv;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.aqr.AQR;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HelperTest {
    @Test
    void testGetAQR() throws URISyntaxException, IOException {
        Optional<AQR> result = Helper.getAQR(new File(Objects.requireNonNull(getClass().getResource("/movies.nj")).toURI()).getAbsolutePath());
        assertTrue(result.isPresent());

        AQR aqr = result.get();
    }
}