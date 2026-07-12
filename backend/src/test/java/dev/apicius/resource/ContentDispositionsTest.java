package dev.apicius.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * FEAT-008 AC3: the download is named from the API's title. The ASCII {@code filename} must be
 * safe as an HTTP quoted-string and as a filename on common filesystems; the RFC 5987
 * {@code filename*} carries the unicode title for browsers that honor it.
 */
class ContentDispositionsTest {

    @Test
    void namesThePlainTitleVerbatim() {
        assertEquals(
                "attachment; filename=\"Payments API.yaml\";"
                        + " filename*=UTF-8''Payments%20API.yaml",
                ContentDispositions.attachment("Payments API", "yaml"));
    }

    // Characters illegal on common filesystems (and the quoted-string breakers " and \)
    // collapse to single spaces; the result carries no escaping problem at all.
    @Test
    void replacesIllegalCharactersWithSpaces() {
        assertEquals(
                "attachment; filename=\"Payments API v2.json\";"
                        + " filename*=UTF-8''Payments%20API%20v2.json",
                ContentDispositions.attachment("Payments/API: \"v2\"", "json"));
    }

    // Non-ASCII drops out of the plain filename (it cannot travel in a quoted-string) but
    // survives percent-encoded in filename*.
    @Test
    void keepsUnicodeInTheExtendedFilenameOnly() {
        assertEquals(
                "attachment; filename=\"Payments API v2.yaml\";"
                        + " filename*=UTF-8''Payments%20API%20v2%20%E2%9C%A8.yaml",
                ContentDispositions.attachment("Payments/API: \"v2\" ✨", "yaml"));
    }

    @Test
    void fallsBackWhenNothingUsableRemains() {
        assertEquals(
                "attachment; filename=\"api.yaml\";"
                        + " filename*=UTF-8''%E6%94%AF%E4%BB%98.yaml",
                ContentDispositions.attachment("支付", "yaml"));
        assertEquals(
                "attachment; filename=\"api.json\"; filename*=UTF-8''api.json",
                ContentDispositions.attachment("...", "json"));
        assertEquals(
                "attachment; filename=\"api.json\"; filename*=UTF-8''api.json",
                ContentDispositions.attachment("  ", "json"));
    }

    @Test
    void stripsControlCharactersAndTrailingDots() {
        assertEquals(
                "attachment; filename=\"Payments API.yaml\";"
                        + " filename*=UTF-8''Payments%20API.yaml",
                ContentDispositions.attachment("Pay\u0007ments API.", "yaml"));
    }
}
