#pragma once

#include <string>
#include "client_messages.pb.h"

namespace io::game::net {

struct ClientMessageEnvelope {
    std::string sessionId;
    ClientMessage message;
};

}
