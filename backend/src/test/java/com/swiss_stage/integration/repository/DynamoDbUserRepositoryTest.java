package com.swiss_stage.integration.repository;

import com.swiss_stage.domain.model.User;
import com.swiss_stage.infrastructure.repository.DynamoDbUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DynamoDbUserRepositoryの統合テスト
 * 
 * DynamoDB Localを使用してリポジトリの動作を検証
 * テストデータ: ユーザーの保存、取得、削除
 */
@SpringBootTest
@ActiveProfiles("test")
class DynamoDbUserRepositoryTest {

    @Autowired
    private DynamoDbUserRepository repository;

    @Autowired
    private DynamoDbClient dynamoDbClient;

    private static final String TABLE_NAME = "swiss_stage_table";
    private static final UUID TEST_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String TEST_GOOGLE_ID = "google-123456";

    @BeforeEach
    void setUp() {
        // テーブルが存在しない場合は作成
        createTableIfNotExists();
        
        // テストデータをクリーンアップ
        cleanupTestData();
    }

    @Test
    void save_shouldPersistUserToDynamoDB() {
        // Given
        User user = User.create(TEST_USER_ID, TEST_GOOGLE_ID, "test@example.com", "Test User");

        // When
        repository.save(user);

        // Then
        Optional<User> savedUser = repository.findById(TEST_USER_ID);
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(savedUser.get().getGoogleId()).isEqualTo(TEST_GOOGLE_ID);
        assertThat(savedUser.get().getDisplayName()).isEqualTo("Test User");
    }

    @Test
    void findById_shouldReturnEmptyWhenUserDoesNotExist() {
        // When
        Optional<User> result = repository.findById(UUID.randomUUID());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByGoogleId_shouldReturnUserWhenExists() {
        // Given
        User user = User.create(TEST_USER_ID, TEST_GOOGLE_ID, "test@example.com", "Test User");
        repository.save(user);

        // When
        Optional<User> result = repository.findByGoogleId(TEST_GOOGLE_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getGoogleId()).isEqualTo(TEST_GOOGLE_ID);
    }

    @Test
    void findByGoogleId_shouldReturnEmptyWhenUserDoesNotExist() {
        // When
        Optional<User> result = repository.findByGoogleId("non-existent-google-id");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void deleteById_shouldRemoveUserFromDynamoDB() {
        // Given
        User user = User.create(TEST_USER_ID, TEST_GOOGLE_ID, "test@example.com", "Test User");
        repository.save(user);

        // When
        repository.deleteById(TEST_USER_ID);

        // Then
        Optional<User> deletedUser = repository.findById(TEST_USER_ID);
        assertThat(deletedUser).isEmpty();
    }

    @Test
    void save_shouldUpdateExistingUser() {
        // Given
        User user = User.create(TEST_USER_ID, TEST_GOOGLE_ID, "test@example.com", "Test User");
        repository.save(user);

        // When - ユーザーを更新
        user.updateLastLoginAt(Instant.now());
        repository.save(user);

        // Then
        Optional<User> updatedUser = repository.findById(TEST_USER_ID);
        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.get().getLastLoginAt()).isAfter(updatedUser.get().getCreatedAt());
    }

    private void createTableIfNotExists() {
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder()
                    .tableName(TABLE_NAME)
                    .build());
        } catch (ResourceNotFoundException e) {
            // テーブルが存在しない場合は作成
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName(TABLE_NAME)
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("PK")
                                    .keyType(KeyType.HASH)
                                    .build(),
                            KeySchemaElement.builder()
                                    .attributeName("SK")
                                    .keyType(KeyType.RANGE)
                                    .build()
                    )
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("PK")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("SK")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("GSI1PK")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("GSI1SK")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .globalSecondaryIndexes(
                            GlobalSecondaryIndex.builder()
                                    .indexName("GSI1")
                                    .keySchema(
                                            KeySchemaElement.builder()
                                                    .attributeName("GSI1PK")
                                                    .keyType(KeyType.HASH)
                                                    .build(),
                                            KeySchemaElement.builder()
                                                    .attributeName("GSI1SK")
                                                    .keyType(KeyType.RANGE)
                                                    .build()
                                    )
                                    .projection(Projection.builder()
                                            .projectionType(ProjectionType.ALL)
                                            .build())
                                    .provisionedThroughput(ProvisionedThroughput.builder()
                                            .readCapacityUnits(5L)
                                            .writeCapacityUnits(5L)
                                            .build())
                                    .build()
                    )
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build())
                    .build();

            dynamoDbClient.createTable(request);

            // テーブルがアクティブになるまで待機
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void cleanupTestData() {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("PK", AttributeValue.builder().s("USER#" + TEST_USER_ID).build());
            key.put("SK", AttributeValue.builder().s("PROFILE").build());

            dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(key)
                    .build());
        } catch (Exception e) {
            // データが存在しない場合は無視
        }
    }
}
