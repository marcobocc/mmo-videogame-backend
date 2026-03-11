#pragma once

#include <chrono>
#include <fmt/format.h>
#include <jwt-cpp/jwt.h>
#include <jwt-cpp/traits/nlohmann-json/traits.h>
#include <stdexcept>
#include <string>

namespace io::game::net {

/**
 * Exception thrown when JWT validation fails.
 * This includes signature verification failures, expired tokens, missing claims, etc.
 */
class JwtValidationException final : public std::runtime_error {
public:
    explicit JwtValidationException(const std::string& msg) : std::runtime_error(msg) {
    }
};

/**
 * Configuration properties for JWT validation.
 */
struct JwtValidatorProperties {
    std::string secret;
    std::string issuer;
    std::chrono::seconds clockSkew{0};
};

/**
 * JWT validator using HS256 algorithm.
 * Validates JWT tokens by verifying signature, issuer (if configured), and subject claim.
 * Thread-safe for concurrent validation calls with the same instance.
 */
class JwtValidator {
public:
    /**
     * Constructs a JWT validator with the given properties.
     * @param props Validation properties including secret, issuer, and clock skew
     * @throws std::invalid_argument if secret is empty
     */
    explicit JwtValidator(JwtValidatorProperties props) : props_(std::move(props)) {
        if (props_.secret.empty()) {
            throw std::invalid_argument("JWT secret cannot be empty");
        }
    }

    /**
     * Validates a JWT token and returns the subject claim.
     * @param token JWT token string in standard format (header.payload.signature)
     * @return The subject (sub) claim from the validated token
     * @throws JwtValidationException if validation fails for any reason
     */
    [[nodiscard]] std::string validate(const std::string& token) const {
        using JwtTraits = jwt::traits::nlohmann_json;
        try {
            const auto decoded = jwt::decode<JwtTraits>(token);

            auto verifier = jwt::verify<JwtTraits>()
                                    .allow_algorithm(jwt::algorithm::hs256{props_.secret})
                                    .leeway(props_.clockSkew.count());

            if (!props_.issuer.empty())
                verifier = verifier.with_issuer(props_.issuer);

            verifier.verify(decoded);

            if (!decoded.has_subject())
                throw JwtValidationException("JWT missing 'sub' claim");

            return decoded.get_subject();
        } catch (const JwtValidationException&) {
            throw;
        } catch (const std::exception& e) {
            throw JwtValidationException(fmt::format("JWT validation failed: {}", e.what()));
        }
    }

private:
    const JwtValidatorProperties props_;
};

}
