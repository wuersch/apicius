package dev.apicius.document;

import java.util.List;

/**
 * How a list capability pages (FEAT-010) — projected only where paging applies (Browse); the
 * facet is {@code null} on every other capability, never empty. {@code on} is derived
 * structurally: the operation carries the {@code page}/{@code limit} query parameters and the
 * wrapper its {@code pagination} member — no marker, no extension (AC7). {@code conflicts}
 * names designer-authored query parameters claiming {@code page}/{@code limit} (UC5): while
 * one exists, enabling is blocked, and the client can say why.
 */
public record PagingFacetView(boolean on, List<String> conflicts) {
}
