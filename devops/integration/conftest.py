def pytest_addoption(parser):
    parser.addoption(
        "--auth-service-url",
        action="store",
        default="http://127.0.0.1:8080/auth",
        help="API URL for the authentication service"
    )
    parser.addoption(
        "--game-service-url",
        action="store",
        default="ws://127.0.0.1:8080/ws/game",
        help="WebSocket URL for the game service"
    )

# Example usage:
#       --auth-service-url=http://localhost:8080 --game-service-url=ws://localhost:8080/ws/game
