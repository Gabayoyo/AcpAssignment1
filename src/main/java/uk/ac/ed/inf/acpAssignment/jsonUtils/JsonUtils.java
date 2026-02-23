package uk.ac.ed.inf.acpAssignment.jsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.List;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class JsonUtils {
  private ObjectMapper objectMapper = new ObjectMapper();

  // singular response to json node
  public JsonNode responseToJsonNode(ResponseInputStream<GetObjectResponse> response) throws Exception {
    return objectMapper.readTree(response);
  }

  // utilises the above method to convert a list of responses to an array node
  public ArrayNode responsesToJsonArray(List<ResponseInputStream<GetObjectResponse>> responses) throws Exception {
    ArrayNode arrayNode = objectMapper.createArrayNode();
    for (ResponseInputStream<GetObjectResponse> response : responses) {
      arrayNode.add(responseToJsonNode(response));
    }
    return arrayNode;
  }

  public ArrayNode stringsToJsonArray(List<String> strings) {
    return objectMapper.valueToTree(strings);
  }

}

