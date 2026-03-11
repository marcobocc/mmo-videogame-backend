#pragma once

#include <boost/beast/http.hpp>
#include <string>
#include <string_view>
#include "networking/JwtValidator.hpp"

namespace io::game::net {
namespace http = boost::beast::http;

/**
 * Exception thrown when authentication fails during WebSocket handshake.
 */
class AuthenticationException final : public std::runtime_error {
public:
    explicit AuthenticationException(const std::string& msg) : std::runtime_error(msg) {
    }
};

/**
 * WebSocket handshake interceptor that validates JWT tokens from Authorization header.
 *
 * Extracts and validates JWT tokens from the "Authorization: Bearer <token>" header
 * during WebSocket upgrade. On successful validation, places the authenticated user ID
 * into the attributes map for downstream use.
 */
struct JwtHandshakeInterceptor {
    static constexpr std::string_view BEARER_PREFIX = "Bearer ";
    static constexpr std::string_view USER_ID_KEY = "userId";

    explicit JwtHandshakeInterceptor(JwtValidator& validator) : validator_(validator) {
    }

    /**
     * Validates JWT token from request and extracts user ID into attributes.
     *
     * @param request HTTP upgrade request containing Authorization header
     * @param attributes Map to store extracted user ID (overwrites existing userId if present)
     * @throws AuthenticationException if Authorization header is missing or malformed
     * @throws JwtValidationException if token validation fails
     */
    void beforeUpgrade(const http::request<http::string_body>& request,
                       std::unordered_map<std::string, std::string>& attributes) const {
        const auto it = request.find(http::field::authorization);
        if (it == request.end()) {
            throw AuthenticationException("Missing Authorization header");
        }

        const auto auth_header = std::string_view(it->value());
        if (!auth_header.starts_with(BEARER_PREFIX)) {
            throw AuthenticationException("Authorization header must use Bearer scheme");
        }

        const auto token = auth_header.substr(BEARER_PREFIX.size());
        const std::string user_id = validator_.validate(std::string(token));
        attributes.insert_or_assign(std::string(USER_ID_KEY), user_id);
    }

private:
    JwtValidator& validator_;
};

}
