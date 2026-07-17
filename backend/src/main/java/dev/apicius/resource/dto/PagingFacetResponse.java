package dev.apicius.resource.dto;

import dev.apicius.document.PagingFacetView;
import java.util.List;

/**
 * How a list capability pages (FEAT-010): the structural on/off state, plus the
 * designer-authored parameter names blocking enable (UC5) so the client can say why.
 */
public record PagingFacetResponse(boolean on, List<String> conflicts) {

    public static PagingFacetResponse from(PagingFacetView view) {
        return new PagingFacetResponse(view.on(), view.conflicts());
    }
}
