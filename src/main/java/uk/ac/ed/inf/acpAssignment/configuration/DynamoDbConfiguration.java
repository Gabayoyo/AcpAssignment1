package uk.ac.ed.inf.acpAssignment.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DynamoDbConfiguration {

    @Value( "${ACP_DYNAMODB:http://dynamodb.localhost.localstack.cloud:4566}")
    private String dynamoDbEndpoint;

    @Bean(name = "dynamoDbEndpoint")
    public String getDynamoDbEndpoint(){
        return dynamoDbEndpoint;
    }
}
