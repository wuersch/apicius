package dev.apicius.resource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds {@code Content-Disposition: attachment} headers for exports (FEAT-008 AC3): the file
 * is named from the API's title, sanitized for header and filesystem safety.
 */
final class ContentDispositions {

    private ContentDispositions() {
    }

    /**
     * {@code attachment; filename="<ascii>.<ext>"; filename*=UTF-8''<rfc5987>.<ext>} — the
     * plain filename for every consumer (illegal and non-ASCII characters removed, so the
     * quoted-string never needs escaping), the RFC 5987 one carrying the unicode title for
     * browsers that honor it.
     */
    static String attachment(String title, String extension) {
        String sanitized = sanitize(title);
        return "attachment; filename=\"" + ascii(sanitized) + "." + extension + "\";"
                + " filename*=UTF-8''" + rfc5987(sanitized) + "." + extension;
    }

    /**
     * Filesystem-safe: control characters go, the characters Windows/POSIX reserve (which
     * include the header-breaking {@code "} and {@code \}) become spaces, whitespace
     * collapses, trailing dots go (Windows) — an emptied name falls back to {@code api}.
     */
    private static String sanitize(String title) {
        String cleaned = title
                .replaceAll("\\p{Cntrl}", "")
                .replaceAll("[/\\\\:*?\"<>|]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .replaceAll("[. ]+$", "");
        return cleaned.isEmpty() ? "api" : cleaned;
    }

    private static String ascii(String sanitized) {
        String ascii = sanitized
                .replaceAll("[^\\x20-\\x7E]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return ascii.isEmpty() ? "api" : ascii;
    }

    /** RFC 5987 percent-encoding — URLEncoder is it, except for its space-as-plus legacy. */
    private static String rfc5987(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
