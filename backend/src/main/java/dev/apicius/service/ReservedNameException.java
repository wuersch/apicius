package dev.apicius.service;

/**
 * A header name Apicius manages (FEAT-011 UC5): {@code Accept}/{@code Content-Type} belong
 * to content negotiation, {@code Authorization} to the future security feature. Reserved
 * regardless of document state — unlike the {@code page}/{@code limit} conflict, which only
 * a paged capability raises — so it maps to <strong>400</strong>, not 409.
 */
public class ReservedNameException extends RuntimeException {

    public ReservedNameException(String detail) {
        super(detail);
    }
}
