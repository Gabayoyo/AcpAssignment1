package uk.ac.ed.inf.acpAssignment.dto;

public record SplitterRequest(
    String readQueue,
    String writeTopicOdd,
    String redisHashOdd,
    String writeTopicEven,
    String redisHashEven,
    int messageCount
) {}