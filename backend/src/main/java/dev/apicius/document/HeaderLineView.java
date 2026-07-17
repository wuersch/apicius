package dev.apicius.document;

/**
 * One line of the Headers facet (FEAT-009 AC3). {@code derived} marks lines Apicius supplies
 * (the content-negotiation line — no corresponding parameter exists in the document);
 * designer-authored headers arrive with FEAT-011.
 */
public record HeaderLineView(String name, String value, boolean derived) {
}
