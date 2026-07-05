package dev.apicius.document.apitomy;

import dev.apicius.document.DocumentEngine;
import dev.apicius.document.SpecVersion;
import io.apitomy.datamodels.Library;
import io.apitomy.datamodels.models.ModelType;
import io.apitomy.datamodels.models.openapi.OpenApiInfo;
import io.apitomy.datamodels.models.openapi.v3x.OpenApi3xDocument;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The {@code apitomy-data-models} adapter (ADR-0009) — the only class allowed to import
 * {@code io.apitomy.*}.
 */
@ApplicationScoped
public class ApitomyDocumentEngine implements DocumentEngine {

    @Override
    public String createEmptyDocument(SpecVersion version, String title, String description) {
        OpenApi3xDocument document = (OpenApi3xDocument) Library.createDocument(toModelType(version));
        // Pin the exact latest patch (FEAT-003 AC1/AC3) — Apicius policy, not the library default.
        document.setOpenapi(version.latestPatch());
        OpenApiInfo info = document.createInfo();
        info.setTitle(title);
        info.setVersion("1.0.0");
        if (description != null) {
            info.setDescription(description);
        }
        document.setInfo(info);
        return Library.writeDocumentToJSONString(document);
    }

    private static ModelType toModelType(SpecVersion version) {
        return switch (version) {
            case V3_0 -> ModelType.OPENAPI30;
            case V3_1 -> ModelType.OPENAPI31;
            case V3_2 -> ModelType.OPENAPI32;
        };
    }
}
