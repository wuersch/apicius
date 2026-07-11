package dev.apicius.document.derivation;

import java.util.ArrayList;
import java.util.List;

/**
 * ADR-0011's property-name derivation: freeform field-name input → the JSON property name
 * that is the field's identity and display. An assist, not a gate — deliberate identifier
 * conventions are respected, never "corrected": spaces trigger camelCase joining, identifier
 * characters pass through, everything else is stripped (visibly, in the live preview).
 *
 * <p>Pure and deterministic; never throws — {@code ""} is a valid, rejectable output (the
 * caller's AC9 rule). Kept honest against the frontend mirror
 * ({@code src/lib/fieldDerivation.ts}) by the shared vectors in
 * {@code src/test/resources/derivation/field-vectors.json}.
 */
public final class FieldNameDerivation {

    private FieldNameDerivation() {
    }

    /**
     * Derives the property name: split on spaces, strip non-identifier characters per word,
     * drop words that strip to nothing, then camelCase-join — the leading word is lowercased
     * whole when all-caps ("API key" → {@code apiKey}), otherwise only its first letter
     * ("First name" → {@code firstName}); later words uppercase their first letter and keep
     * the rest ("user API key" → {@code userAPIKey}). A single already-conventional
     * identifier ({@code first_name}, {@code firstName}) passes through unchanged.
     */
    public static String derive(String rawName) {
        List<String> words = new ArrayList<>();
        for (String word : rawName.split(" ")) {
            String cleaned = stripToIdentifierCharacters(word);
            if (!cleaned.isEmpty()) {
                words.add(cleaned);
            }
        }
        if (words.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder(leadingWord(words.get(0)));
        for (String word : words.subList(1, words.size())) {
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    // By code point, not char: supplementary-plane letters (surrogate pairs) must survive
    // exactly like the frontend mirror's /\p{L}\p{Nd}_/u does — pinned by the vectors.
    private static String stripToIdentifierCharacters(String word) {
        StringBuilder cleaned = new StringBuilder();
        word.codePoints()
                .filter(cp -> Character.isLetterOrDigit(cp) || cp == '_')
                .forEach(cleaned::appendCodePoint);
        return cleaned.toString();
    }

    /** An all-caps leading word is an acronym starting the name — lowercased whole (ADR-0011). */
    private static String leadingWord(String word) {
        if (isAllCaps(word)) {
            return word.toLowerCase();
        }
        return Character.toLowerCase(word.charAt(0)) + word.substring(1);
    }

    private static boolean isAllCaps(String word) {
        boolean hasLetter = false;
        for (int i = 0; i < word.length(); ) {
            int cp = word.codePointAt(i);
            if (Character.isLowerCase(cp)) {
                return false;
            }
            hasLetter |= Character.isLetter(cp);
            i += Character.charCount(cp);
        }
        return hasLetter;
    }
}
