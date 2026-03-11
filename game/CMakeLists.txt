cmake_minimum_required(VERSION 3.22)
project(game LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

add_subdirectory(common)
add_subdirectory(game_server)
add_custom_target(build_all
        COMMAND ${CMAKE_CTEST_COMMAND} --output-on-failure
        DEPENDS game_server
        COMMENT "Building and running all tests"
)

# Testing
enable_testing()
add_subdirectory(tests)