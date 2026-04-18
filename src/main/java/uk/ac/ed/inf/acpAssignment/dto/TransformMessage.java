package uk.ac.ed.inf.acpAssignment.dto;

public record TransformMessage(
    String key,
    int version,
    double value
) {}
