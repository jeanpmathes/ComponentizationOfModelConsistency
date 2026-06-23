package tools.vitruv.compmodelcons.generator.backend;

import tools.vitruv.neojoin.aqr.AQR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Generator {
    private final AQR aqr;

    public Generator(AQR aqr) {
        this.aqr = aqr;
    }

    public String generate() {
        return "// Generated";
    }
}
