package uk.ac.ed.inf.acpAssignment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Slf4j
@Service
public class JsonService {

  // singular response to json node
  public JsonNode responseToJsonNode(ResponseInputStream<GetObjectResponse> response) throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readTree(response);
  }

  // utilises the above method to convert a list of responses to an array node
  public ArrayNode responsesToJsonArray(List<ResponseInputStream<GetObjectResponse>> responses) throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode arrayNode = objectMapper.createArrayNode();
    for (ResponseInputStream<GetObjectResponse> response : responses) {
      arrayNode.add(responseToJsonNode(response));
    }
    return arrayNode;
  }

}

