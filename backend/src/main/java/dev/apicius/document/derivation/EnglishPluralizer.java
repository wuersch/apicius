package dev.apicius.document.derivation;

import java.util.Map;

/**
 * The small built-in English pluralizer path derivation couples to (ADR-0010). Deliberately
 * tiny: an uncovered irregular yields a wrong-but-consistent plural until per-resource path
 * overrides arrive. No f→ves rule — chef→chefs would break more than it fixes.
 */
final class EnglishPluralizer {

    /**
     * Singular → plural. A word that already <em>is</em> one of these plurals returns itself,
     * so Person and People derive the same {@code /people} path — the collision is then caught
     * by the uniqueness check instead of writing two resources onto one path.
     */
    private static final Map<String, String> IRREGULARS = Map.of(
            "person", "people", "child", "children", "man", "men", "woman", "women",
            "foot", "feet", "tooth", "teeth", "mouse", "mice", "goose", "geese");

    private EnglishPluralizer() {
    }

    /** Pluralizes one word, preserving the case of its first letter ("Person" → "People"). */
    static String pluralize(String word) {
        String lower = word.toLowerCase();
        if (IRREGULARS.containsValue(lower)) {
            return word;
        }
        String irregular = IRREGULARS.get(lower);
        if (irregular != null) {
            return matchFirstLetterCase(irregular, word);
        }
        if (lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z")
                || lower.endsWith("ch") || lower.endsWith("sh")) {
            return word + "es";
        }
        if (lower.length() > 1 && lower.endsWith("y") && !isVowel(lower.charAt(lower.length() - 2))) {
            return word.substring(0, word.length() - 1) + "ies";
        }
        return word + "s";
    }

    private static String matchFirstLetterCase(String plural, String original) {
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(plural.charAt(0)) + plural.substring(1);
        }
        return plural;
    }

    private static boolean isVowel(char c) {
        return "aeiou".indexOf(c) >= 0;
    }
}
