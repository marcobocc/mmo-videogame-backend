import uuid
import pytest
from python_client import PythonClient

# ----------------------------
# Fixtures
# ----------------------------
@pytest.fixture
def new_user():
    return f"player_{uuid.uuid4().hex[:8]}"

@pytest.fixture
def python_client(request):
    auth_service_url = request.config.getoption("--auth-service-url")
    game_service_url = request.config.getoption("--game-service-url")
    client = PythonClient(auth_service_url=auth_service_url, game_service_url=game_service_url)
    return client

@pytest.fixture
def registered_user(new_user, python_client):
    python_client.register_user(new_user, "secret")
    jwt = python_client.authenticate_user(new_user, "secret")
    return {"username": new_user, "jwt": jwt}

# ----------------------------
# Tests
# ----------------------------

def test_register_new_user_success(new_user, python_client):
    resp = python_client.register_user(new_user, "secret")
    assert resp.get("message") == "User registered successfully"


def test_register_existing_user_error(registered_user, python_client):
    username = registered_user["username"]

    with pytest.raises(Exception):
        python_client.register_user(username, "secret")


def test_authenticate_existing_user_success(registered_user):
    jwt = registered_user["jwt"]
    assert jwt and isinstance(jwt, str)


def test_authenticate_invalid_credentials_error(new_user, python_client):
    python_client.register_user(new_user, "secret")
    with pytest.raises(Exception):
        python_client.authenticate_user(new_user, "wrongpassword")

def test_connect_to_game_service(new_user, python_client):
    python_client.register_user(new_user, "secret")
    python_client.connect_to_game_service(new_user, "secret")
    assert python_client._ws_app is not None
    test_message = "Test message"
    received_message = python_client.echo_message(test_message, timeout_secs=5)
    assert received_message == f"Echo: {test_message}"
