#!/usr/bin/env python3

import subprocess
import sys
import argparse
import os
import venv


GREEN = "\033[92m"
CYAN = "\033[96m"
RESET = "\033[0m"

VENV_DIR = "./devops/.venv"
REQUIREMENTS_FILE = "./devops/requirements.txt"
INGRESS_NAMESPACE = "ingress-nginx"
INGRESS_SERVICE = "ingress-nginx-controller"


def run_cmd(cmd, check=True, capture_output=False, env=None):
    print(f"Running: {' '.join(cmd)}")
    return subprocess.run(cmd, check=check, capture_output=capture_output, text=True, env=env)


def check_prerequisites():
    prerequisites = [
        ("docker", "--version"),
        ("minikube", "version"),
        ("kubectl", "version", "--client"),
        ("python3", "--version"),
    ]
    for cmd in prerequisites:
        try:
            run_cmd(cmd, capture_output=True)
        except subprocess.CalledProcessError:
            print(f"Error: {cmd[0]} is not installed or not in PATH.")
            sys.exit(1)


def setup_venv():
    if not os.path.exists(VENV_DIR):
        print(f"Creating virtual environment in {VENV_DIR}...")
        venv.create(VENV_DIR, with_pip=True)
    if os.name == "nt":
        # Windows
        python_bin = os.path.join(VENV_DIR, "Scripts", "python.exe")
        pip_bin = os.path.join(VENV_DIR, "Scripts", "pip.exe")
    else:
        # Unix / Mac
        python_bin = os.path.join(VENV_DIR, "bin", "python3")
        pip_bin = os.path.join(VENV_DIR, "bin", "pip")

    print("Installing dependencies from requirements.txt...")
    run_cmd([pip_bin, "install", "-r", REQUIREMENTS_FILE])

    return python_bin


def run_deploy_minikube(python_bin, service=None, teardown=False):
    cmd = [python_bin, "devops/deploy_minikube.py"]
    if service:
        cmd += ["--service", service]
    if teardown:
        cmd += ["--teardown"]
    run_cmd(cmd)
    print(f"{GREEN}✅ Deployment completed successfully!{RESET}")


def run_integration_tests(python_bin):
    print("Port-forwarding ingress controller...")
    pf_process = subprocess.Popen(
        ["kubectl", "port-forward", "--namespace", INGRESS_NAMESPACE,
         f"svc/{INGRESS_SERVICE}", "8080:80", "8443:443"]
    )
    try:
        print("Running integration tests...")
        run_cmd([
            python_bin, "-m", "pytest", "-v", "devops/integration",
            "--auth-service-url=http://localhost:8080/auth",
            "--game-service-url=ws://localhost:8080/ws/game"
        ])
        print(f"{CYAN}✅ Integration tests passed successfully!{RESET}")
    finally:
        pf_process.terminate()


def main():
    parser = argparse.ArgumentParser(description="Deploy backend locally on Minikube and run integration tests")
    parser.add_argument("--service", help="Deploy only a specific service (auth or game)")
    parser.add_argument("--teardown", action="store_true", help="Delete existing cluster")
    args = parser.parse_args()

    check_prerequisites()
    python_bin = setup_venv()
    run_deploy_minikube(python_bin, service=args.service, teardown=args.teardown)
    run_integration_tests(python_bin)


if __name__ == "__main__":
    main()
