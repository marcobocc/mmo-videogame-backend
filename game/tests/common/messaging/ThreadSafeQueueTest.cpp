#include <gtest/gtest.h>
#include <thread>
#include "messaging/ThreadSafeQueue.hpp"

using namespace io::game::net;

class ThreadSafeQueueTest : public testing::Test {
protected:
    ThreadSafeQueue<int> queue_;
};

TEST_F(ThreadSafeQueueTest, PushAndPopSingleElement) {
    queue_.push(42);
    const auto result = queue_.pop();
    ASSERT_TRUE(result.has_value());
    EXPECT_EQ(result.value(), 42);
}

TEST_F(ThreadSafeQueueTest, PopFromEmptyQueue) {
    const auto result = queue_.pop();
    EXPECT_FALSE(result.has_value());
}

TEST_F(ThreadSafeQueueTest, MultipleElements) {
    queue_.push(1);
    queue_.push(2);
    queue_.push(3);

    const auto result1 = queue_.pop();
    ASSERT_TRUE(result1.has_value());
    EXPECT_EQ(result1.value(), 1);

    const auto result2 = queue_.pop();
    ASSERT_TRUE(result2.has_value());
    EXPECT_EQ(result2.value(), 2);

    const auto result3 = queue_.pop();
    ASSERT_TRUE(result3.has_value());
    EXPECT_EQ(result3.value(), 3);
}

TEST_F(ThreadSafeQueueTest, SizeMethod) {
    EXPECT_EQ(queue_.size(), 0);

    queue_.push(1);
    EXPECT_EQ(queue_.size(), 1);

    queue_.push(2);
    EXPECT_EQ(queue_.size(), 2);

    queue_.pop();
    EXPECT_EQ(queue_.size(), 1);

    queue_.pop();
    EXPECT_EQ(queue_.size(), 0);
}

TEST_F(ThreadSafeQueueTest, FIFOOrder) {
    for (int i = 0; i < 100; ++i) {
        queue_.push(i);
    }

    for (int i = 0; i < 100; ++i) {
        auto result = queue_.pop();
        ASSERT_TRUE(result.has_value());
        EXPECT_EQ(result.value(), i);
    }
}

TEST_F(ThreadSafeQueueTest, MoveSemantics) {
    struct MoveOnlyType {
        int value;
        explicit MoveOnlyType(const int v) : value(v) {
        }
        MoveOnlyType(const MoveOnlyType&) = delete;
        MoveOnlyType(MoveOnlyType&&) = default;
    };

    ThreadSafeQueue<MoveOnlyType> move_queue;
    move_queue.push(MoveOnlyType(42));

    const auto result = move_queue.pop();
    ASSERT_TRUE(result.has_value());
    EXPECT_EQ(result.value().value, 42);
}

TEST_F(ThreadSafeQueueTest, StringQueue) {
    ThreadSafeQueue<std::string> string_queue;

    string_queue.push("hello");
    string_queue.push("world");

    const auto result1 = string_queue.pop();
    ASSERT_TRUE(result1.has_value());
    EXPECT_EQ(result1.value(), "hello");

    const auto result2 = string_queue.pop();
    ASSERT_TRUE(result2.has_value());
    EXPECT_EQ(result2.value(), "world");
}
