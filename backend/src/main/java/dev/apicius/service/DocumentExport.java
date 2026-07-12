package dev.apicius.service;

/** A named export: the serialized document plus the title that names the file (FEAT-008 AC3). */
public record DocumentExport(String title, String content) {
}
