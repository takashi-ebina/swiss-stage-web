package com.swiss_stage.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

/**
 * DynamoDB設定クラス
 * ローカル開発環境とAWS本番環境の両方に対応
 */
@Configuration
public class DynamoDbConfig {

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.dynamodb.endpoint:}")
    private String dynamoDbEndpoint;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(awsRegion));

        // ローカル開発（DynamoDB Local）が指定されている場合のみダミーの静的認証情報を使う
        if (dynamoDbEndpoint != null && !dynamoDbEndpoint.isBlank()) {
            String endpointLower = dynamoDbEndpoint.toLowerCase();
            boolean isLocalhost = endpointLower.contains("localhost") || endpointLower.contains("127.0.0.1");
            if (isLocalhost) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummy", "dummy")
                ));
                builder.endpointOverride(URI.create(dynamoDbEndpoint));
            } else {
                // 外部/本番のエンドポイントが指定されている場合は実環境の認証情報を使う
                builder.credentialsProvider(DefaultCredentialsProvider.create());
                builder.endpointOverride(URI.create(dynamoDbEndpoint));
            }
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
