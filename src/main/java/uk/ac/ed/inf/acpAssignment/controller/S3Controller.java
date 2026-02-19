package uk.ac.ed.inf.acpAssignment.controller;

import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.model.S3Object;
import uk.ac.ed.inf.acpAssignment.configuration.S3Configuration;

import java.net.URI;
import java.util.List;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import uk.ac.ed.inf.acpAssignment.configuration.SystemEnvironment;
import uk.ac.ed.inf.acpAssignment.service.S3Service;

@RestController()
@RequestMapping("/api/v1/acp/s3")
public class S3Controller {

    private final S3Configuration s3Configuration;
    private final S3Service s3Service;

    public S3Controller(S3Configuration s3Configuration, S3Service s3Service) {
        this.s3Configuration = s3Configuration;
        this.s3Service = s3Service;
    }


    @GetMapping("/endpoint")
    public String getS3Endpoint() {
        return s3Configuration.getS3Endpoint();
    }

    @GetMapping("/buckets")
    public List<String> listBuckets() {
        return s3Service.listBuckets();
    }

    @GetMapping("/list-objects/{bucket}")
    public List<String> listBucketObjects(@PathVariable String bucket) {
        return s3Service.listBucketObjects(bucket);
    }

    @PutMapping("/create-bucket/{bucket}")
    public void createBucket(@PathVariable String bucket) {
        s3Service.createBucket(bucket);
    }

    @PutMapping("/create-object/{bucket}/{s3Object}")
    public void createBucket(@PathVariable String bucket, @PathVariable String s3Object, @RequestBody String objectContent) {
        s3Service.createBucket(bucket, s3Object, objectContent);
    }
}
