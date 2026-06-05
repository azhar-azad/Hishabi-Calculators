package dev.azhar.hishabi.calculators.tax.model;

import java.time.OffsetDateTime;
import tools.jackson.databind.JsonNode;

public record TaxHistoryItemResponse(
        Long id, OffsetDateTime createdAt, JsonNode request, JsonNode response) {}
