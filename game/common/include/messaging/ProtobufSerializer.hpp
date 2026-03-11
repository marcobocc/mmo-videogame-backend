#pragma once

#include <fmt/core.h>
#include <google/protobuf/message.h>
#include <stdexcept>
#include <string>

namespace io::game::net {

struct ProtobufSerializer {
    static std::string serialize(const google::protobuf::Message& msg) {
        std::string out;
        if (!msg.SerializeToString(&out))
            throw std::runtime_error(fmt::format("Failed to serialize message: {}", msg.GetDescriptor()->full_name()));
        return out;
    }

    template<typename T>
    static T deserialize(const std::string& data) {
        static_assert(std::is_base_of_v<google::protobuf::Message, T>, "T must be a protobuf message type");
        T msg;
        if (!msg.ParseFromString(data))
            throw std::runtime_error(
                    fmt::format("Failed to deserialize message: {}", msg.GetDescriptor()->full_name()));
        return msg;
    }
};

}
