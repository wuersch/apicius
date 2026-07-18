package dev.apicius.document.derivation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FEAT-011's header-name derivation: freeform input → the Hyphenated-Capitalized header name
 * that is the declaration's identity and display. {@link FieldNameDerivation}'s sibling with a
 * header-shaped stance: hyphens and underscores are segment boundaries alongside spaces (typed
 * hyphens <em>are</em> the joiner; underscores are a proxy footgun a best-practices editor
 * never emits), and only ASCII letters and digits survive — header names are RFC 9110 tokens,
 * so Unicode that field derivation would keep is stripped (visibly, in the live preview).
 * Casing <em>within</em> a segment is kept as typed, so {@code X-Request-ID} and acronyms like
 * {@code API} ride through while missing capitalization is supplied.
 *
 * <p>Pure and deterministic; never throws — {@code ""} is a valid, rejectable output (the
 * caller's AC6 rule). Kept honest against the frontend mirror
 * ({@code src/lib/headerDerivation.ts}) by the shared vectors in
 * {@code src/test/resources/derivation/declaration-vectors.json}.
 */
public final class HeaderNameDerivation {

    private HeaderNameDerivation() {
    }

    /**
     * Derives the header name: split on spaces, hyphens, and underscores; strip each segment
     * to ASCII letters and digits; drop segments that strip to nothing; uppercase each
     * segment's first character keeping the rest as typed (a no-op on a digit); join with
     * {@code -}.
     */
    public static String derive(String rawName) {
        List<String> segments = new ArrayList<>();
        for (String segment : rawName.split("[ \\-_]")) {
            String cleaned = stripToTokenCharacters(segment);
            if (!cleaned.isEmpty()) {
                segments.add(cleaned);
            }
        }
        return segments.stream()
                .map(segment -> Character.toUpperCase(segment.charAt(0)) + segment.substring(1))
                .collect(Collectors.joining("-"));
    }

    private static String stripToTokenCharacters(String segment) {
        StringBuilder cleaned = new StringBuilder();
        for (char c : segment.toCharArray()) {
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                cleaned.append(c);
            }
        }
        return cleaned.toString();
    }
}
