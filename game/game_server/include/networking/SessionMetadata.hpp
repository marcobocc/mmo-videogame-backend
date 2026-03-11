#pragma once

#include <boost/uuid/uuid.hpp>
#include <boost/uuid/uuid_generators.hpp>
#include <boost/uuid/uuid_io.hpp>
#include <fmt/format.h>
#include <mutex>
#include <optional>
#include <stdexcept>
#include <string>
#include <unordered_map>
#include <unordered_set>

namespace io::game::net {

/**
 * Metadata container for WebSocket sessions.
 *
 * Stores session-specific information including a unique session ID, creation timestamp,
 * and custom key-value pairs. Session ID and creation timestamp are immutable reserved keys.
 *
 * Thread-safe: Construction is thread-safe. Individual getter/setter operations are not synchronized
 * and should be externally synchronized if accessed from multiple threads after construction.
 */
class SessionMetadata {
public:
    /**
     * Constructs metadata with unique session ID and creation timestamp.
     * Thread-safe: Can be called concurrently from multiple threads.
     */
    SessionMetadata() {
        static boost::uuids::random_generator gen;
        static std::mutex gen_mutex;

        std::string session_id;
        {
            std::lock_guard lock(gen_mutex);
            session_id = boost::uuids::to_string(gen());
        }

        metadata_[SESSION_ID_KEY] = std::move(session_id);
        metadata_[CREATED_AT_KEY] = std::to_string(std::time(nullptr));
    }

    SessionMetadata(const SessionMetadata&) = default;
    SessionMetadata& operator=(const SessionMetadata&) = default;
    SessionMetadata(SessionMetadata&&) noexcept = default;
    SessionMetadata& operator=(SessionMetadata&&) noexcept = default;

    /**
     * Sets a custom metadata key-value pair.
     * Overwrites existing value if key already exists.
     *
     * @param key Metadata key (cannot be "sessionId" or "createdAt")
     * @param value Metadata value
     * @throws std::invalid_argument if key is reserved
     */
    void set(const std::string_view key, const std::string_view value) {
        if (RESERVED_KEYS.contains(key)) {
            throw std::invalid_argument(fmt::format("{} is a reserved metadata key and cannot be modified", key));
        }
        metadata_.insert_or_assign(std::string(key), std::string(value));
    }

    /**
     * Sets multiple metadata key-value pairs from a map.
     * Overwrites existing values if keys already exist.
     *
     * @param kv Map of key-value pairs to set
     * @throws std::invalid_argument if any key is reserved
     */
    void set(const std::unordered_map<std::string, std::string>& kv) {
        for (const auto& [key, value]: kv) {
            set(key, value);
        }
    }

    /**
     * Gets a metadata value by key.
     *
     * @param key Metadata key to retrieve
     * @return Optional containing value if key exists, empty otherwise
     */
    [[nodiscard]] std::optional<std::string> get(const std::string_view key) const {
        if (const auto it = metadata_.find(std::string(key)); it != metadata_.end()) {
            return it->second;
        }
        return std::nullopt;
    }

    /**
     * Gets the unique session ID.
     *
     * @return UUID string in standard format (e.g., "550e8400-e29b-41d4-a716-446655440000")
     */
    [[nodiscard]] std::string sessionId() const {
        return metadata_.at(SESSION_ID_KEY);
    }

    /**
     * Gets the creation timestamp.
     *
     * @return Unix timestamp as string
     */
    [[nodiscard]] std::string createdAt() const {
        return metadata_.at(CREATED_AT_KEY);
    }

private:
    static constexpr auto SESSION_ID_KEY = "sessionId";
    static constexpr auto CREATED_AT_KEY = "createdAt";
    inline static const std::unordered_set<std::string_view> RESERVED_KEYS = {SESSION_ID_KEY, CREATED_AT_KEY};

    std::unordered_map<std::string, std::string> metadata_;
};
}
