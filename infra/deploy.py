#!/usr/bin/env python3
import subprocess
import sys
import os

NAMESPACE = "default"
K8S_DIR = "./k8s"

def run(cmd, capture_output=False, check=True):
    print(f"Running: {' '.join(cmd)}")
    return subprocess.run(cmd, capture_output=capture_output, text=True, check=check)

def ensure_minikube_running():
    try:
        status_result = run(["minikube", "status", "--format={{.Host}}"], capture_output=True)
        minikube_status = status_result.stdout.strip()
    except subprocess.CalledProcessError:
        minikube_status = "Stopped"

    if minikube_status != "Running":
        print("Minikube is not running. Starting Minikube...")
        run(["minikube", "start", "--driver=docker"])
    else:
        print("Minikube is already running.")

def deploy_auth_service():
    TARGET_IMAGE_NAME = "auth-app:latest"
    DEPLOYMENT_NAME = "auth-app"
    APP_LABEL = "auth-app"
    SERVICE_DIR = "../auth_service"

    print("\n=== Deploying Auth Service ===")
    print(f"Building Docker image {TARGET_IMAGE_NAME}...")
    run(["docker", "build", "-t", TARGET_IMAGE_NAME, SERVICE_DIR])
    run(["minikube", "image", "load", TARGET_IMAGE_NAME])

    k8s_manifests = ["secrets.yaml", "auth.yaml"]
    print(f"Applying Kubernetes manifests: {k8s_manifests}")
    for manifest in k8s_manifests:
        manifest_path = os.path.join(K8S_DIR, manifest)
        if os.path.exists(manifest_path):
            print(f"Applying manifest: {manifest}")
            run(["kubectl", "apply", "-f", manifest_path])
        else:
            print(f"Warning: {manifest_path} does not exist. Skipping.")

    print(f"Restarting deployment {DEPLOYMENT_NAME}...")
    run(["kubectl", "rollout", "restart", "deployment", DEPLOYMENT_NAME, "-n", NAMESPACE])

    print(f"Waiting for pods with label app={APP_LABEL}...")
    run([
        "kubectl", "rollout", "status",
        f"deployment/{DEPLOYMENT_NAME}",
        "-n", NAMESPACE,
        "--timeout=120s"
    ])

    print("Auth Service deployed successfully!\n")

if __name__ == "__main__":
    try:
        ensure_minikube_running()
        deploy_auth_service()
    except subprocess.CalledProcessError as e:
        print(f"Command failed: {e.cmd}")
        sys.exit(1)
