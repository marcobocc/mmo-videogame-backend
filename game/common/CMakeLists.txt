cmake_minimum_required(VERSION 3.22)

find_package(Protobuf REQUIRED)
find_package(nlohmann_json CONFIG REQUIRED)

set(PROTO_FILES
        ${CMAKE_SOURCE_DIR}/common/proto/client_messages.proto
        ${CMAKE_SOURCE_DIR}/common/proto/server_messages.proto
)

set(GENERATED_PROTO_DIR ${CMAKE_BINARY_DIR}/common/generated)
file(MAKE_DIRECTORY ${GENERATED_PROTO_DIR})

protobuf_generate_cpp(
        PROTO_SRCS
        PROTO_HDRS
        ${PROTO_FILES}
        PROTOC_OUT_DIR ${GENERATED_PROTO_DIR}
)

file(GLOB_RECURSE COMMON_SOURCES src/*.cpp)

add_library(common STATIC
        ${COMMON_SOURCES}
        ${PROTO_SRCS}
)

target_include_directories(common
        PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${GENERATED_PROTO_DIR}
)

target_link_libraries(common
        PUBLIC
        nlohmann_json::nlohmann_json
        protobuf::libprotobuf
)

add_library(game::common ALIAS common)