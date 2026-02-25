package uk.ac.ed.inf.acpAssignment.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record DynamoObject(String key, JsonNode content) {}
