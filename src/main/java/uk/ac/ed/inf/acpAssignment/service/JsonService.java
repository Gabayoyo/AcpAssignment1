package uk.ac.ed.inf.acpAssignment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.List;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class JsonService {

  public static ArrayNode responsesToJsonArray(List<ResponseInputStream<GetObjectResponse>> responses) throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode arrayNode = objectMapper.createArrayNode();
    for (ResponseInputStream<GetObjectResponse> response : responses) {
      arrayNode.add(objectMapper.readTree(response));
    }
    return arrayNode;
  }

}

