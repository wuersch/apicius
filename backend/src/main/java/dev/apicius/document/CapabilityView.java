package dev.apicius.document;

import dev.apicius.document.derivation.Capability;

/**
 * One recognized capability of a resource, as the editor displays it: the plain-language label
 * (the operation's {@code summary}, canonical fallback when absent) plus the derived
 * method/path detail, de-emphasized in the UI (PRIN-002).
 */
public record CapabilityView(Capability capability, String label, String method, String path) {
}
