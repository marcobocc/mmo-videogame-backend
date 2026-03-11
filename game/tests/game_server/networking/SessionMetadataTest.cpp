#include <gtest/gtest.h>
#include <regex>
#include <thread>
#include <unordered_map>
#include "networking/SessionMetadata.hpp"

using namespace io::game::net;

class SessionMetadataTest : public testing::Test {
protected:
    SessionMetadata metadata_;
};

TEST_F(SessionMetadataTest, DefaultConstruction) {
    const std::string session_id = metadata_.sessionId();
    EXPECT_FALSE(session_id.empty());
}

TEST_F(SessionMetadataTest, SessionIdIsUUID) {
    const std::string session_id = metadata_.sessionId();
    const std::regex uuid_regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", std::regex::icase);
    EXPECT_TRUE(std::regex_match(session_id, uuid_regex));
}

TEST_F(SessionMetadataTest, CreatedAtIsSet) {
    const std::string created_at = metadata_.createdAt();
    EXPECT_FALSE(created_at.empty());

    const long long timestamp = std::stoll(created_at);
    const long long current_time = std::time(nullptr);
    constexpr long long year_2020 = 1577836800;

    EXPECT_GT(timestamp, year_2020);
    EXPECT_LE(timestamp, current_time + 1);
}

TEST_F(SessionMetadataTest, SetAndGetCustomMetadata) {
    constexpr std::string key = "userId";
    constexpr std::string value = "user-12345";
    metadata_.set(key, value);

    const auto result = metadata_.get(key);
    ASSERT_TRUE(result.has_value());
    EXPECT_EQ(result.value(), value);
}

TEST_F(SessionMetadataTest, SetMultipleCustomMetadata) {
    metadata_.set("userId", "user-123");
    metadata_.set("username", "john_doe");
    metadata_.set("region", "us-west");

    EXPECT_EQ(metadata_.get("userId").value(), "user-123");
    EXPECT_EQ(metadata_.get("username").value(), "john_doe");
    EXPECT_EQ(metadata_.get("region").value(), "us-west");
}

TEST_F(SessionMetadataTest, SetFromMap) {
    const std::unordered_map<std::string, std::string> attributes
            = {{"userId", "user-456"}, {"username", "jane_doe"}, {"ipAddress", "192.168.1.1"}};
    metadata_.set(attributes);

    EXPECT_EQ(metadata_.get("userId").value(), "user-456");
    EXPECT_EQ(metadata_.get("username").value(), "jane_doe");
    EXPECT_EQ(metadata_.get("ipAddress").value(), "192.168.1.1");
}

TEST_F(SessionMetadataTest, GetNonExistentKey) {
    const auto value = metadata_.get("nonexistent");
    EXPECT_FALSE(value.has_value());
}

TEST_F(SessionMetadataTest, CannotModifySessionId) {
    EXPECT_THROW(metadata_.set("sessionId", "invalid-id"), std::invalid_argument);
}

TEST_F(SessionMetadataTest, CannotModifyCreatedAt) {
    EXPECT_THROW(metadata_.set("createdAt", "999999999"), std::invalid_argument);
}

TEST_F(SessionMetadataTest, EachMetadataInstanceHasUniqueSessionId) {
    const SessionMetadata metadata1;
    const SessionMetadata metadata2;

    const std::string session_id_1 = metadata1.sessionId();
    const std::string session_id_2 = metadata2.sessionId();

    EXPECT_NE(session_id_1, session_id_2);
}

TEST_F(SessionMetadataTest, MetadataWithStringView) {
    constexpr std::string_view key = "custom_key";
    constexpr std::string_view value = "custom_value";
    metadata_.set(key, value);
    EXPECT_EQ(metadata_.get(key).value(), "custom_value");
}

TEST_F(SessionMetadataTest, EmptyMapDoesNotThrow) {
    const std::unordered_map<std::string, std::string> empty_attributes;
    EXPECT_NO_THROW(metadata_.set(empty_attributes));
    EXPECT_FALSE(metadata_.sessionId().empty());
}

TEST_F(SessionMetadataTest, OverwritesExistingMetadata) {
    metadata_.set("userId", "user-123");
    EXPECT_EQ(metadata_.get("userId").value(), "user-123");

    metadata_.set("userId", "user-456");
    EXPECT_EQ(metadata_.get("userId").value(), "user-456");
}

TEST_F(SessionMetadataTest, MapSetThrowsOnReservedKeys) {
    const std::unordered_map<std::string, std::string> attributes_with_reserved
            = {{"userId", "user-123"}, {"sessionId", "invalid-id"}};

    EXPECT_THROW(metadata_.set(attributes_with_reserved), std::invalid_argument);
}

TEST_F(SessionMetadataTest, MapSetPartiallyAppliesBeforeThrow) {
    const std::unordered_map<std::string, std::string> attributes
            = {{"userId", "user-123"}, {"username", "john"}, {"sessionId", "invalid-id"}};

    try {
        metadata_.set(attributes);
        FAIL() << "Expected std::invalid_argument to be thrown";
    } catch (const std::invalid_argument&) {
    }

    const auto user_id = metadata_.get("userId");
    const auto username = metadata_.get("username");

    EXPECT_TRUE(user_id.has_value() || username.has_value());
}
