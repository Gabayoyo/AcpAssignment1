package uk.ac.ed.inf.acpAssignment.dto;

public record TransformRequest(String readQueue, String writeQueue, int messageCount) {

}
