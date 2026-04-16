package uk.ac.ed.inf.acpAssignment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.gson.Gson;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import uk.ac.ed.inf.acpAssignment.configuration.SystemEnvironment;
import uk.ac.ed.inf.acpAssignment.dto.Drone;
import uk.ac.ed.inf.acpAssignment.dto.Restaurant;
import uk.ac.ed.inf.acpAssignment.dto.Tuple;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import uk.ac.ed.inf.acpAssignment.dto.UrlPath;
import uk.ac.ed.inf.acpAssignment.service.KafkaService;
import uk.ac.ed.inf.acpAssignment.service.RabbitMqService;
import uk.ac.ed.inf.acpAssignment.utils.DroneUtils;
import uk.ac.ed.inf.acpAssignment.utils.JsonUtils;
import uk.ac.ed.inf.acpAssignment.service.DynamoDbService;
import uk.ac.ed.inf.acpAssignment.service.PostgresService;
import uk.ac.ed.inf.acpAssignment.service.S3Service;

@RestController()

// Provide a default namespace
@RequestMapping("/api/v1/acp")

// Can be used, yet sometimes problematic
// @AllArgsConstructor

public class CoreRestController {

    // Is deprecated
    // @Autowired
    private final SystemEnvironment acpSystemEnvironment;
    private final S3Service s3Service;
    private final JsonUtils jsonUtils;
    private final DynamoDbService dynamoDbService;
    private final PostgresService postgresService;
    private final RabbitMqService rabbitMqService;
    private final KafkaService kafkaService;

    /**
     * Retrieves the ILP service endpoint URL from the system environment.
     *
     * @return the ILP service endpoint URL as a string
     */
    @GetMapping("/ilp-endpoint")
    public String getIlpServiceEndpoint() {
        return acpSystemEnvironment.getIlpServiceEndpoint();
    }

    /**
     * Retrieves a configuration value from the system environment.
     *
     * @param endpoint the configuration value key
     * @return the configuration value as a string
     */
    @GetMapping("/config-value")
    public String getConfigValue(@Value("#{ilpServiceEndpoint}") String endpoint) {
        return endpoint;
    }

    /**
     * Constructs a new CoreRestController instance with the provided environment.
     *
     * @param acpSystemEnvironment the system environment
     */
    public CoreRestController(SystemEnvironment acpSystemEnvironment, S3Service s3Service,
        DynamoDbService dynamoDbService, PostgresService postgresService,
        RabbitMqService rabbitMqService, KafkaService kafkaService) {
        this.acpSystemEnvironment = acpSystemEnvironment;
        this.s3Service = s3Service;
        this.jsonUtils = new JsonUtils();
        this.dynamoDbService = dynamoDbService;
        this.postgresService = postgresService;
        this.rabbitMqService = rabbitMqService;
        this.kafkaService = kafkaService;
    }

    /**
     * get a buffered reader for a resource
     *
     * @param jsonResource the JSON resource this reader is required for
     * @return the buffered reader
     */
    private java.io.BufferedReader getBufferedReaderForResource(String jsonResource) {
        return new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(jsonResource))));
    }

    /**
     * returns the restaurants in the system
     *
     * @return array of suppliers
     */
    @GetMapping("/restaurants")
    public Restaurant[] restaurants() {
        return new Gson().fromJson(getBufferedReaderForResource("json/restaurants.json"), Restaurant[].class);
    }

    /**
     * simple test method to test the service's availability
     *
     * @param input an optional input which will be echoed
     * @return the echo
     */
    @GetMapping(value = {"/testPath/{input}", "/testPath"})
    public String test(@PathVariable(required = false) String input) {
        return String.format("Hello from the ILP-Tutorial-REST-Service. Your provided value was: %s", input == null ? "not provided" : input);
    }

    /**
     * GET with HTML result
     * @return
     */
    @RequestMapping(value = "/test", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String testHtml() {
        return """
                <html>
                <header><title>ILP Tutorial REST Server</title></header>
                <body>
                <h1>Hello from the ILP Tutorial REST Server
                </h1></body>
                </html>""";
    }

    /**
     * POST with a JSON data structure in the request body
     * @param postAttribute
     * @return
     */
    @PostMapping(value = "/testPostBody",  consumes = {"*/*"})
    public String testPost(@RequestBody Tuple postAttribute) {
        return "You posted: " + postAttribute.toString();
    }

    /**
     * POST with request parameters
     * @param item1
     * @param item2
     * @return
     */
    @PostMapping("/testPostPath")
    public String testPost(@RequestParam("item1") String item1, @RequestParam("item2") String item2) {
        var postAttribute = new Tuple(item1, item2);
        return "You posted: " + postAttribute.toString();
    }

    @GetMapping("/all/s3/{bucket}")
    public ResponseEntity<?> getBucketObjects(@PathVariable String bucket) {
        try {
            List<ResponseInputStream<GetObjectResponse>> bucketContents =
                s3Service.listBucketContents(bucket);
            ArrayNode ObjectJsonArray = jsonUtils.responsesToJsonArray(bucketContents);
            return new ResponseEntity<>(ObjectJsonArray, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/single/s3/{bucket}/{key}")
    public ResponseEntity<?> getBucketObjectWithKey(@PathVariable String bucket,
        @PathVariable String key) {
        try {
            ResponseInputStream<GetObjectResponse> objectContent = s3Service.getObjectContent(bucket, key);
            JsonNode objectJson = jsonUtils.responseToJsonNode(objectContent);
            return new ResponseEntity<>(objectJson, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/all/dynamo/{table}")
    public ResponseEntity<?> getDynamoTableContents(@PathVariable String table) {
        try {
            var tableContents = dynamoDbService.listTableContents(table);
            ArrayNode contentsJsonArray = jsonUtils.stringsToJsonArray(tableContents);
            return new ResponseEntity<>(contentsJsonArray, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/single/dynamo/{table}/{key}")
    public ResponseEntity<?> getDynamoObjectWithKey(@PathVariable String table,
        @PathVariable String key) {
        try {
            String objectContent = dynamoDbService.getObjectContent(table, key);
            JsonNode objectJson = jsonUtils.stringToJsonNode(objectContent);
            return new ResponseEntity<>(objectJson, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/all/postgres/{table}")
    public ResponseEntity<List<Map<String, Object>>> getPostgresTableContents(@PathVariable String table) {
        try {
            var rows = postgresService.getRows(table);
            return new ResponseEntity<>(rows, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/postgres/current-schema")
    public ResponseEntity<?> getCurrentSchema() {
        try {
            var currentSchema = postgresService.currentSchema();
            return new ResponseEntity<>(currentSchema, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/postgres/schemas")
    public ResponseEntity<?> listSchemas() {
        try {
            var schemas = postgresService.listSchemas();
            return new ResponseEntity<>(schemas, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/process/dump")
    public ResponseEntity<?> processDump(@RequestBody UrlPath urlPath) {
        try {
            Drone[] drones = jsonUtils.readUrlToDrones(new URL(urlPath.urlPath()));
            Drone[] computedDrones = DroneUtils.computeDronesCostPer100Moves(drones);
            return new ResponseEntity<>(computedDrones, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/process/dynamo")
    public ResponseEntity<?> processDynamo(@RequestBody UrlPath urlPath) {
        try {
              Drone[] drones = jsonUtils.readUrlToDrones(new URL(urlPath.urlPath()));
                Drone[] computedDrones = DroneUtils.computeDronesCostPer100Moves(drones);
                dynamoDbService.addDronesToTable(computedDrones);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/process/s3")
    public ResponseEntity<?> processS3(@RequestBody UrlPath urlPath) {
        try {
            Drone[] drones = jsonUtils.readUrlToDrones(new URL(urlPath.urlPath()));
            Drone[] computedDrones = DroneUtils.computeDronesCostPer100Moves(drones);
            s3Service.addDronesToBucket(computedDrones);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/process/postgres/{table}")
    public ResponseEntity<?> processPostgres(@PathVariable String table,
        @RequestBody UrlPath urlPath) {
        try {
            Drone[] drones = jsonUtils.readUrlToDrones(new URL(urlPath.urlPath()));
            Drone[] computedDrones = DroneUtils.computeDronesCostPer100Moves(drones);
            postgresService.addDronesToTable(computedDrones, table);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/copy-content/dynamo/{table}")
    public ResponseEntity<?> copyContentToDynamo(@PathVariable String table) {
        try {
            var rows = postgresService.getRows(table);
            dynamoDbService.createObjects(rows);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/copy-content/s3/{table}")
    public ResponseEntity<?> copyContentToS3(@PathVariable String table) {
        try {
            var rows = postgresService.getRows(table);
            s3Service.addDroneObjectsToBucket(rows);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
     }

     @PutMapping("messages/rabbitmq/{queueName}/{messageCount}")
    public ResponseEntity<?> sendMessagesRabbit(@PathVariable String queueName,
         @PathVariable int messageCount) {
        try {
            rabbitMqService.sendMessages(queueName, messageCount);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
     }

     @GetMapping("messages/rabbitmq/{queueName}/{timeoutInMsec}")
    public ResponseEntity<?> getMessagesRabbit(@PathVariable String queueName,
        @PathVariable long timeoutInMsec) {
        try {
            List<String> messages = rabbitMqService.getMessages(queueName, timeoutInMsec);
            return new ResponseEntity<>(messages, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
     }

     @GetMapping("messages/sorted/rabbitmq/{queueName}/{messagesToConsider}")
    public ResponseEntity<?> getMessagesToConsider(@PathVariable String queueName,
         @PathVariable int messagesToConsider) {
        try {
            List<String> messages = rabbitMqService.readSortedMessages(queueName, messagesToConsider);
            return  new ResponseEntity<>(messages, HttpStatus.OK);
        } catch  (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
     }

     @PutMapping("messages/kafka/{writeTopic}/{messageCount}")
    public ResponseEntity<?> sendMessagesKafka(@PathVariable String writeTopic,
        @PathVariable int messageCount) {
        try {
            kafkaService.sendMessages(writeTopic, messageCount);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
     }

     @GetMapping("messages/kafka/{readTopic}/{timeoutInMsec}")
    public ResponseEntity<?> getMessagesKafka(@PathVariable String readTopic,
        @PathVariable long timeoutInMsec) {
        try {

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
     }
}
