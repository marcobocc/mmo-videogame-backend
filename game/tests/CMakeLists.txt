file(GLOB_RECURSE TEST_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/*.cpp)
add_executable(tests ${TEST_SOURCES})

find_package(GTest CONFIG REQUIRED)
find_package(Boost REQUIRED COMPONENTS system thread)
find_package(jwt-cpp CONFIG REQUIRED)
find_package(fmt CONFIG REQUIRED)

target_include_directories(tests
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/../game_server/include
        ${CMAKE_CURRENT_SOURCE_DIR}/../common/include
        ${CMAKE_BINARY_DIR}/common/generated
)

target_link_libraries(tests PRIVATE
        game::common
        Boost::system
        Boost::thread
        jwt-cpp::jwt-cpp
        fmt::fmt
        GTest::gtest
        GTest::gtest_main
)

add_test(NAME UnitTests COMMAND tests)