package tools.vitruv.compmodelcons.generator.tools;

import tools.vitruv.neojoin.aqr.AQR;

public class NamingGenerator {
    private NamingGenerator() {

    }

    public static String convertToPascalCase(String name) {
        StringBuilder builder = new StringBuilder();

        boolean shouldNextBeUpperCase = true;
        for (char c : name.toCharArray()) {
            if (Character.isLetter(c)) {
                if (shouldNextBeUpperCase) {
                    builder.append(Character.toUpperCase(c));
                    shouldNextBeUpperCase = false;
                } else {
                    builder.append(c);
                }
            } else if (Character.isDigit(c)) {
                builder.append(c);
                shouldNextBeUpperCase = true;
            } else if (Character.isWhitespace(c) || c == '_') {
                shouldNextBeUpperCase = true;
            }
        }

        return builder.toString();
    }

    public static String getPackageName(AQR aqr) {
        return String.format("neojoin.viewtypes.%s", aqr.export().name());
    }

    public static String getPackagePath(AQR aqr) {
        return String.format("neojoin/viewtypes/%s", aqr.export().name());
    }
}
