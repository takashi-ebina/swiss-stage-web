package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.User;
import com.swiss_stage.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * DynamoDbUserRepository実装（Infrastructure層）
 * UserRepositoryインターフェースの実装
 * 憲章原則I「ドメイン駆動設計」に準拠
 */
@Repository
public class DynamoDbUserRepository implements UserRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbUserRepository(
            DynamoDbClient dynamoDbClient,
            @Value("${aws.dynamodb.table-name}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public Optional<User> findById(UUID userId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("PK", AttributeValue.builder().s("USER#" + userId.toString()).build());
        key.put("SK", AttributeValue.builder().s("METADATA").build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        try {
            GetItemResponse response = dynamoDbClient.getItem(request);
            if (!response.hasItem()) {
                return Optional.empty();
            }
            return Optional.of(mapToUser(response.item()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to find user by ID: " + userId, e);
        }
    }

    @Override
    public Optional<User> findByGoogleId(String googleId) {
        // Note: This implementation uses Scan for simplicity.
        // For production, consider adding a GSI on googleId for better performance.
        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("googleId = :googleId AND SK = :sk")
                .expressionAttributeValues(Map.of(
                        ":googleId", AttributeValue.builder().s(googleId).build(),
                        ":sk", AttributeValue.builder().s("METADATA").build()
                ))
                .build();

        try {
            ScanResponse response = dynamoDbClient.scan(request);
            if (response.items().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(mapToUser(response.items().get(0)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to find user by Google ID: " + googleId, e);
        }
    }

    @Override
    public User save(User user) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("PK", AttributeValue.builder().s("USER#" + user.getUserId().toString()).build());
        item.put("SK", AttributeValue.builder().s("METADATA").build());
        item.put("userId", AttributeValue.builder().s(user.getUserId().toString()).build());
        item.put("googleId", AttributeValue.builder().s(user.getGoogleId()).build());
        item.put("email", AttributeValue.builder().s(user.getEmail()).build());
        item.put("displayName", AttributeValue.builder().s(user.getDisplayName()).build());
        item.put("createdAt", AttributeValue.builder().n(String.valueOf(user.getCreatedAt().toEpochMilli())).build());
        item.put("lastLoginAt", AttributeValue.builder().n(String.valueOf(user.getLastLoginAt().toEpochMilli())).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        try {
            dynamoDbClient.putItem(request);
            return user;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save user: " + user.getUserId(), e);
        }
    }

    @Override
    public void deleteById(UUID userId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("PK", AttributeValue.builder().s("USER#" + userId.toString()).build());
        key.put("SK", AttributeValue.builder().s("METADATA").build());

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        try {
            dynamoDbClient.deleteItem(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete user: " + userId, e);
        }
    }

    /**
     * DynamoDB ItemをUserエンティティにマッピング
     */
    private User mapToUser(Map<String, AttributeValue> item) {
        UUID userId = UUID.fromString(item.get("userId").s());
        String googleId = item.get("googleId").s();
        String email = item.get("email").s();
        String displayName = item.get("displayName").s();
        Instant createdAt = Instant.ofEpochMilli(Long.parseLong(item.get("createdAt").n()));
        Instant lastLoginAt = Instant.ofEpochMilli(Long.parseLong(item.get("lastLoginAt").n()));

        return User.create(userId, googleId, email, displayName);
    }
}
