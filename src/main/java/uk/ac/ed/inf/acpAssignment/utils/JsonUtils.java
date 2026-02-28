package uk.ac.ed.inf.acpAssignment.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import uk.ac.ed.inf.acpAssignment.dto.Drone;

public class JsonUtils {
  private final ObjectMapper objectMapper = new ObjectMapper();

  // singular response to json node
  public JsonNode responseToJsonNode(ResponseInputStream<GetObjectResponse> response) throws Exception {
    String body = new String(response.readAllBytes(), StandardCharsets.UTF_8);
    try {
      return objectMapper.readTree(body);
    } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
      return TextNode.valueOf(body);
    }
  }

  // utilises the above method to convert a list of responses to an array node
  public ArrayNode responsesToJsonArray(List<ResponseInputStream<GetObjectResponse>> responses) throws Exception {
    ArrayNode arrayNode = objectMapper.createArrayNode();
    for (ResponseInputStream<GetObjectResponse> response : responses) {
      arrayNode.add(responseToJsonNode(response));
    }
    return arrayNode;
  }

  public ArrayNode stringsToJsonArray(List<String> strings) throws Exception {
    ArrayNode arrayNode = objectMapper.createArrayNode();
    for (String string : strings) {
      try {
        arrayNode.add(objectMapper.readTree(string));
      } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
        arrayNode.add(TextNode.valueOf(string));
      }
    }
    return arrayNode;
  }

  public JsonNode stringToJsonNode(String string) throws Exception {
    try {
      return objectMapper.readTree(string);
    } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
      return TextNode.valueOf(string);
    }
  }

  public Drone[] readUrlToDrones(URL url) throws Exception {
    return objectMapper.readValue(url, Drone[].class);
  }

  public boolean isValidJson(String string) {
    try {
      objectMapper.readTree(string);
      return true;
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      return false;
    }
  }

}

