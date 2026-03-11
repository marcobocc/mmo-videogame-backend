file(GLOB_RECURSE TEST_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/*.cpp)
add_executable(tests ${TEST_SOURCES})

find_package(GTest CONFIG REQUIRED)
target_link_libraries(tests PRIVATE
        game::common
        GTest::gtest
        GTest::gtest_main
)

add_test(NAME UnitTests COMMAND tests)