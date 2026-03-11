#include <chrono>
#include <gtest/gtest.h>
#include "networking/JwtValidator.hpp"

using namespace io::game::net;

class JwtValidatorTest : public testing::Test {
protected:
    const std::string secret_ = "test-256-bit-secret-key-must-be-long-enough-for-hs256";
    const std::string issuer_ = "test-issuer";
    const std::chrono::seconds clock_skew_{5};

    static JwtValidatorProperties createProperties(const std::string& secret,
                                                   const std::string& issuer = "",
                                                   const std::chrono::seconds skew = std::chrono::seconds{0}) {
        return JwtValidatorProperties{secret, issuer, skew};
    }

    [[nodiscard]] std::string createTokenWithExpiration(const std::string& subject,
                                                        const std::chrono::seconds expires_in) const {
        const auto now = std::chrono::system_clock::now();
        const auto exp_time = now + expires_in;
        return jwt::create<jwt::traits::nlohmann_json>().set_subject(subject).set_expires_at(exp_time).sign(
                jwt::algorithm::hs256{secret_});
    }

    [[nodiscard]] std::string createTokenWithNotBefore(const std::string& subject,
                                                       const std::chrono::seconds not_before_offset) const {
        const auto now = std::chrono::system_clock::now();
        const auto nbf_time = now + not_before_offset;
        return jwt::create<jwt::traits::nlohmann_json>().set_subject(subject).set_not_before(nbf_time).sign(
                jwt::algorithm::hs256{secret_});
    }
};

TEST_F(JwtValidatorTest, ConstructorInitializesProperties) {
    const auto props = createProperties(secret_, issuer_);
    const JwtValidator validator(props);
    EXPECT_TRUE(true);
}

TEST_F(JwtValidatorTest, EmptySecretThrowsInvalidArgument) {
    const auto props = createProperties("");
    EXPECT_THROW(JwtValidator validator(props), std::invalid_argument);
}

TEST_F(JwtValidatorTest, ValidJwtWithSubject) {
    const auto props = createProperties(secret_);
    const JwtValidator validator(props);
    const std::string token
            = jwt::create<jwt::traits::nlohmann_json>().set_subject("user-12345").sign(jwt::algorithm::hs256{secret_});

    const std::string subject = validator.validate(token);
    EXPECT_EQ(subject, "user-12345");
}

TEST_F(JwtValidatorTest, ValidJwtWithDifferentSubject) {
    const auto props = createProperties(secret_);
    const JwtValidator validator(props);

    const std::string token = jwt::create<jwt::traits::nlohmann_json>()
                                      .set_subject("another-user")
                                      .sign(jwt::algorithm::hs256{secret_});

    const std::string subject = validator.validate(token);
    EXPECT_EQ(subject, "another-user");
}

TEST_F(JwtValidatorTest, InvalidTokenThrowsException) {
    const auto props = createProperties(secret_);
    const JwtValidator validator(props);

    const char* invalid_token = "invalid.token.here";
    EXPECT_THROW({ (void) validator.validate(invalid_token); }, JwtValidationException);
}

TEST_F(JwtValidatorTest, JwtWithWrongSecretThrowsException) {
    const auto props = createProperties(secret_);
    const JwtValidator validator(props);

    const char* wrong_secret = "wrong-secret-key";
    const std::string token = jwt::create<jwt::traits::nlohmann_json>()
                                      .set_subject("user-12345")
                                      .sign(jwt::algorithm::hs256{wrong_secret});

    EXPECT_THROW({ (void) validator.validate(token); }, JwtValidationException);
}

TEST_F(JwtValidatorTest, JwtMissingSubjectThrowsException) {
    const auto props = createProperties(secret_);
    const JwtValidator validator(props);

    const std::string token = jwt::create<jwt::traits::nlohmann_json>().sign(jwt::algorithm::hs256{secret_});

    EXPECT_THROW({ (void) validator.validate(token); }, JwtValidationException);
}

TEST_F(JwtValidatorTest, JwtValidationExceptionMessage) {
    const auto props = createProperties(secret_);
    const JwtValidator validator(props);

    try {
        (void) validator.validate("invalid.token");
        FAIL() << "Expected JwtValidationException to be thrown";
    } catch (const JwtValidationException& e) {
        const std::string message = e.what();
        EXPECT_TRUE(message.find("JWT validation failed") != std::string::npos);
    }
}

TEST_F(JwtValidatorTest, ValidJwtWithIssuerValidation) {
    const auto props = createProperties(secret_, issuer_);
    const JwtValidator validator(props);

    const std::string token = jwt::create<jwt::traits::nlohmann_json>()
                                      .set_issuer(issuer_)
                                      .set_subject("user-12345")
                                      .sign(jwt::algorithm::hs256{secret_});

    const std::string subject = validator.validate(token);
    EXPECT_EQ(subject, "user-12345");
}

TEST_F(JwtValidatorTest, JwtWithWrongIssuerThrowsException) {
    const auto props = createProperties(secret_, issuer_);
    const JwtValidator validator(props);

    const char* wrong_issuer = "wrong-issuer";
    const std::string token = jwt::create<jwt::traits::nlohmann_json>()
                                      .set_issuer(wrong_issuer)
                                      .set_subject("user-12345")
                                      .sign(jwt::algorithm::hs256{secret_});

    EXPECT_THROW({ (void) validator.validate(token); }, JwtValidationException);
}

TEST_F(JwtValidatorTest, EmptyIssuerSkipsIssuerValidation) {
    const auto props = createProperties(secret_, "");
    const JwtValidator validator(props);

    const std::string token = jwt::create<jwt::traits::nlohmann_json>()
                                      .set_issuer("any-issuer")
                                      .set_subject("user-12345")
                                      .sign(jwt::algorithm::hs256{secret_});

    const std::string subject = validator.validate(token);
    EXPECT_EQ(subject, "user-12345");
}

TEST_F(JwtValidatorTest, JwtWithClockSkew) {
    const auto props = createProperties(secret_, "", clock_skew_);
    const JwtValidator validator(props);

    const std::string token
            = jwt::create<jwt::traits::nlohmann_json>().set_subject("user-12345").sign(jwt::algorithm::hs256{secret_});

    const std::string subject = validator.validate(token);
    EXPECT_EQ(subject, "user-12345");
}

TEST_F(JwtValidatorTest, ValidJwtWithoutOptionalClaims) {
    const auto props = createProperties(secret_);
    const JwtValidator validator(props);

    const std::string token
            = jwt::create<jwt::traits::nlohmann_json>().set_subject("user-12345").sign(jwt::algorithm::hs256{secret_});

    const std::string subject = validator.validate(token);
    EXPECT_EQ(subject, "user-12345");
}

TEST_F(JwtValidatorTest, ExpiredTokenThrowsException) {
    const auto props = createProperties(secret_);
    const JwtValidator validator(props);

    const std::string token = createTokenWithExpiration("user-12345", std::chrono::seconds{-10});

    EXPECT_THROW({ (void) validator.validate(token); }, JwtValidationException);
}

TEST_F(JwtValidatorTest, ExpiredTokenWithinClockSkewIsValid) {
    const auto props = createProperties(secret_, "", std::chrono::seconds{10});
    const JwtValidator validator(props);

    const std::string token = createTokenWithExpiration("user-12345", std::chrono::seconds{-5});

    const std::string subject = validator.validate(token);
    EXPECT_EQ(subject, "user-12345");
}

TEST_F(JwtValidatorTest, NotYetValidTokenThrowsException) {
    const auto props = createProperties(secret_);
    const JwtValidator validator(props);

    const std::string token = createTokenWithNotBefore("user-12345", std::chrono::seconds{10});

    EXPECT_THROW({ (void) validator.validate(token); }, JwtValidationException);
}

TEST_F(JwtValidatorTest, NotYetValidTokenWithinClockSkewIsValid) {
    const auto props = createProperties(secret_, "", std::chrono::seconds{10});
    const JwtValidator validator(props);

    const std::string token = createTokenWithNotBefore("user-12345", std::chrono::seconds{5});

    const std::string subject = validator.validate(token);
    EXPECT_EQ(subject, "user-12345");
}

TEST_F(JwtValidatorTest, EmptyTokenThrowsException) {
    const auto props = createProperties(secret_);
    const JwtValidator validator(props);

    EXPECT_THROW({ (void) validator.validate(""); }, JwtValidationException);
}

TEST_F(JwtValidatorTest, MalformedTokenThrowsException) {
    const auto props = createProperties(secret_);
    const JwtValidator validator(props);

    EXPECT_THROW({ (void) validator.validate("header.payload"); }, JwtValidationException);
}
