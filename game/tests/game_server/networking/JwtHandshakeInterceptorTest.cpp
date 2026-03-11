#include <gtest/gtest.h>
#include <unordered_map>
#include "networking/JwtHandshakeInterceptor.hpp"

using namespace io::game::net;
namespace http = boost::beast::http;

class JwtHandshakeInterceptorTest : public testing::Test {
protected:
    const std::string secret_ = "your-256-bit-secret-key-must-be-long-enough-for-hs256";
    const std::string issuer_ = "test-issuer";

    static JwtValidator createValidator(const std::string& secret
                                        = "your-256-bit-secret-key-must-be-long-enough-for-hs256",
                                        const std::string& issuer = "") {
        return JwtValidator(JwtValidatorProperties{secret, issuer, std::chrono::seconds{0}});
    }

    static http::request<http::string_body> createRequest(const std::string& auth_header) {
        http::request<http::string_body> request;
        request.method(http::verb::get);
        request.target("/");
        request.version(11);
        request.set(http::field::host, "localhost");
        request.set(http::field::authorization, auth_header);
        return request;
    }
};

TEST_F(JwtHandshakeInterceptorTest, SuccessfulBeforeUpgrade) {
    auto validator = createValidator(secret_);
    JwtHandshakeInterceptor interceptor(validator);

    const std::string token
            = jwt::create<jwt::traits::nlohmann_json>().set_subject("user-12345").sign(jwt::algorithm::hs256{secret_});

    const std::string auth_header = "Bearer " + token;
    auto request = createRequest(auth_header);
    std::unordered_map<std::string, std::string> attributes;

    EXPECT_NO_THROW(interceptor.beforeUpgrade(request, attributes));
    EXPECT_EQ(attributes["userId"], "user-12345");
}

TEST_F(JwtHandshakeInterceptorTest, MissingAuthorizationHeader) {
    auto validator = createValidator(secret_);
    JwtHandshakeInterceptor interceptor(validator);

    http::request<http::string_body> request;
    request.method(http::verb::get);
    request.target("/");
    request.version(11);
    request.set(http::field::host, "localhost");

    std::unordered_map<std::string, std::string> attributes;

    EXPECT_THROW(interceptor.beforeUpgrade(request, attributes), AuthenticationException);
}

TEST_F(JwtHandshakeInterceptorTest, MissingBearerPrefix) {
    auto validator = createValidator(secret_);
    JwtHandshakeInterceptor interceptor(validator);

    const std::string token
            = jwt::create<jwt::traits::nlohmann_json>().set_subject("user-12345").sign(jwt::algorithm::hs256{secret_});

    auto request = createRequest(token);
    std::unordered_map<std::string, std::string> attributes;

    EXPECT_THROW(interceptor.beforeUpgrade(request, attributes), AuthenticationException);
}

TEST_F(JwtHandshakeInterceptorTest, InvalidTokenAfterBearer) {
    auto validator = createValidator(secret_);
    JwtHandshakeInterceptor interceptor(validator);

    const std::string auth_header = "Bearer invalid.token.here";
    auto request = createRequest(auth_header);
    std::unordered_map<std::string, std::string> attributes;

    EXPECT_THROW(interceptor.beforeUpgrade(request, attributes), JwtValidationException);
}

TEST_F(JwtHandshakeInterceptorTest, WrongSecretInToken) {
    auto validator = createValidator(secret_);
    JwtHandshakeInterceptor interceptor(validator);

    const char* wrong_secret = "wrong-secret-key";
    const std::string token = jwt::create<jwt::traits::nlohmann_json>()
                                      .set_subject("user-12345")
                                      .sign(jwt::algorithm::hs256{wrong_secret});

    const std::string auth_header = "Bearer " + token;
    auto request = createRequest(auth_header);
    std::unordered_map<std::string, std::string> attributes;

    EXPECT_THROW(interceptor.beforeUpgrade(request, attributes), JwtValidationException);
}

TEST_F(JwtHandshakeInterceptorTest, MultipleBeforeUpgradeCalls) {
    auto validator = createValidator(secret_);
    JwtHandshakeInterceptor interceptor(validator);

    const std::string token1
            = jwt::create<jwt::traits::nlohmann_json>().set_subject("user-123").sign(jwt::algorithm::hs256{secret_});

    const std::string token2
            = jwt::create<jwt::traits::nlohmann_json>().set_subject("user-456").sign(jwt::algorithm::hs256{secret_});

    auto request1 = createRequest("Bearer " + token1);
    auto request2 = createRequest("Bearer " + token2);

    std::unordered_map<std::string, std::string> attributes1;
    std::unordered_map<std::string, std::string> attributes2;

    EXPECT_NO_THROW(interceptor.beforeUpgrade(request1, attributes1));
    EXPECT_NO_THROW(interceptor.beforeUpgrade(request2, attributes2));

    EXPECT_EQ(attributes1["userId"], "user-123");
    EXPECT_EQ(attributes2["userId"], "user-456");
}

TEST_F(JwtHandshakeInterceptorTest, EmptyAuthorizationHeader) {
    auto validator = createValidator(secret_);
    JwtHandshakeInterceptor interceptor(validator);

    auto request = createRequest("");
    std::unordered_map<std::string, std::string> attributes;

    EXPECT_THROW(interceptor.beforeUpgrade(request, attributes), AuthenticationException);
}

TEST_F(JwtHandshakeInterceptorTest, BearerPrefixWithoutToken) {
    auto validator = createValidator(secret_);
    JwtHandshakeInterceptor interceptor(validator);

    const char* auth_header = "Bearer ";
    auto request = createRequest(auth_header);
    std::unordered_map<std::string, std::string> attributes;

    EXPECT_THROW(interceptor.beforeUpgrade(request, attributes), std::exception);
}

TEST_F(JwtHandshakeInterceptorTest, OverwritesExistingUserId) {
    auto validator = createValidator(secret_);
    JwtHandshakeInterceptor interceptor(validator);

    const std::string token
            = jwt::create<jwt::traits::nlohmann_json>().set_subject("user-12345").sign(jwt::algorithm::hs256{secret_});

    const std::string auth_header = "Bearer " + token;
    auto request = createRequest(auth_header);
    std::unordered_map<std::string, std::string> attributes;
    attributes["userId"] = "old-user";

    interceptor.beforeUpgrade(request, attributes);
    EXPECT_EQ(attributes["userId"], "user-12345");
}
