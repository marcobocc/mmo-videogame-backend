#pragma once

#include <deque>
#include <mutex>
#include <optional>

namespace io::game::net {

template<typename T>
class ThreadSafeQueue {
public:
    void push(T item) {
        std::lock_guard lock(mutex_);
        queue_.push_back(std::move(item));
    }

    std::optional<T> pop() {
        std::lock_guard lock(mutex_);
        if (queue_.empty())
            return std::nullopt;
        T item = std::move(queue_.front());
        queue_.pop_front();
        return item;
    }

    size_t size() const {
        std::lock_guard lock(mutex_);
        return queue_.size();
    }

private:
    mutable std::mutex mutex_;
    std::deque<T> queue_;
};

}
