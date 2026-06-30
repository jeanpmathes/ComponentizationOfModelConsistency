package tools.vitruv.compmodelcons.generator.tools;

import org.apache.commons.text.CaseUtils;
import tools.vitruv.neojoin.aqr.AQR;

public class NamingGenerator {
    public static final String PACKAGE_BASE = "neojoin.viewtypes";

    private NamingGenerator() {

    }
    private final static char[] DELIMITERS = {
            '_', ' ', '-'
    };

    public static String convertToPascalCase(String name) {
        return CaseUtils.toCamelCase(name, true, DELIMITERS);
    }

    public static String getPackageName(AQR aqr) {
        return String.format("%s.%s", PACKAGE_BASE, aqr.export().name());
    }

    public static String getPackagePath(AQR aqr) {
        return String.format("neojoin/viewtypes/%s", aqr.export().name());
    }
}
