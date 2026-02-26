package uk.ac.ed.inf.acpAssignment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import uk.ac.ed.inf.acpAssignment.configuration.S3Configuration;
import uk.ac.ed.inf.acpAssignment.configuration.SystemEnvironment;
import uk.ac.ed.inf.acpAssignment.dto.Drone;

@Slf4j
@Service
public class S3Service {

  private final S3Configuration s3Configuration;
  private final SystemEnvironment systemEnvironment;

  public S3Service(S3Configuration s3Configuration, SystemEnvironment systemEnvironment) {
    this.s3Configuration = s3Configuration;
    this.systemEnvironment = systemEnvironment;
  }

  public List<String> listBuckets() {
    return getS3Client().listBuckets().buckets().stream().map(Bucket::name).toList();
  }

  public List<String> listBucketObjects(String bucket) {
    return getS3Client().listObjectsV2(b -> b.bucket(bucket)).contents().stream().map(S3Object::key).toList();
  }

  public List<ResponseInputStream<GetObjectResponse>> listBucketContents(String bucket) {
     var keys = listBucketObjects(bucket);
     var requests =
         keys.stream().map(key -> GetObjectRequest.builder().bucket(bucket).key(key).build()).toList();
      return requests.stream().map(request -> getS3Client().getObject(request)).toList();
  }

  public void addDronesToBucket(Drone[] drones) throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    for (Drone drone : drones) {
      String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(drone);

      PutObjectRequest req = PutObjectRequest.builder()
          .bucket("s2417814")
          .key(drone.name())
          .build();

      getS3Client().putObject(req, RequestBody.fromString(json));
    }
  }

  public void addDroneObjectsToBucket(List<Map<String, Object>> objects) throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    Drone[] drones = new Drone[objects.size()];
    for (Map<String, Object> object : objects) {
      String droneJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
      PutObjectRequest req = PutObjectRequest.builder()
          .bucket("s2417814")
          .key(java.util.UUID.randomUUID().toString())
          .build();

      getS3Client().putObject(req, RequestBody.fromString(droneJson));
    }
  }

  public ResponseInputStream<GetObjectResponse> getObjectContent(String bucket, String key) {
    var request = GetObjectRequest.builder().bucket(bucket).key(key).build();
    return getS3Client().getObject(request);
  }

  public void createBucket(String bucket) {
    getS3Client().createBucket(b -> b.bucket(bucket));
  }

  public void createBucket(String bucket, String s3Object, String objectContent) {
    getS3Client().putObject(b -> b.bucket(bucket).key(s3Object), software.amazon.awssdk.core.sync.RequestBody.fromString(objectContent));
  }

  private S3Client getS3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create(s3Configuration.getS3Endpoint()))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(systemEnvironment.getAwsUser(), systemEnvironment.getAwsSecret())))
        .region(systemEnvironment.getAwsRegion()).serviceConfiguration(c -> c.pathStyleAccessEnabled(true))
        .build();
  }
}
