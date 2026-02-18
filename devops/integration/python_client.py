import requests
from websocket import WebSocketApp
import threading
import queue
import time


class PythonClient:
    def __init__(self, auth_service_url, game_service_url=None, host_header="mmo.local"):
        self._auth_service_url = auth_service_url
        self._game_service_url = game_service_url
        self._host_header = host_header

        self._session = requests.Session()
        self._session.headers.update({
            "Host": host_header,
            "Content-Type": "application/json"
        })

        # WebSocket state (internal)
        self._ws_app = None
        self._ws_thread = None
        self._ws_messages = queue.Queue()

    # ---------------------------
    # HTTP API Methods
    # ---------------------------

    def register_user(self, username, password):
        payload = {"username": username, "password": password}
        r = self._session.post(f"{self._auth_service_url}/register", json=payload)
        r.raise_for_status()
        return r.json()

    def authenticate_user(self, username, password):
        payload = {"username": username, "password": password}
        r = self._session.post(f"{self._auth_service_url}/login", json=payload)
        r.raise_for_status()

        jwt = r.json().get("jwt")
        if not jwt or jwt == "null":
            raise ValueError("JWT not returned in login response")
        return jwt

    # ---------------------------
    # WebSocket Methods
    # ---------------------------

    def connect_to_game_service(self, username, password, wait_before_send=2):
        if not self._game_service_url:
            raise ValueError("WebSocket URL not configured")
        jwt = self.authenticate_user(username, password)
        headers = [f"Authorization: Bearer {jwt}"]
        self._ws_app = WebSocketApp(
            self._game_service_url,
            header=headers,
            on_message=lambda ws, msg: self._ws_messages.put(msg)
        )
        self._ws_thread = threading.Thread(
            target=lambda: self._ws_app.run_forever(
                origin=f"http://{self._host_header}",
                host=self._host_header
            ),
            daemon=True
        )
        self._ws_thread.start()
        time.sleep(wait_before_send)
        return jwt

    def echo_message(self, message, timeout_secs):
        if not self._ws_app:
            raise RuntimeError("WebSocket not connected")
        self._ws_app.send(message)
        return self._ws_messages.get(timeout=timeout_secs)

    def close(self):
        if self._ws_app:
            self._ws_app.close()
        if self._ws_thread:
            self._ws_thread.join(timeout=2)
        self._ws_app = None
        self._ws_thread = None
