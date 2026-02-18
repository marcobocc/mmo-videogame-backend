#!/usr/bin/env python3

import argparse
import subprocess
import sys
from pathlib import Path
from typing import List


NAMESPACE = "default"
K8S_DIR = Path("./k8s")


def run(cmd: List[str], capture_output: bool = False) -> subprocess.CompletedProcess:
    print(f"$ {' '.join(cmd)}")
    return subprocess.run(
        cmd,
        check=True,
        text=True,
        capture_output=capture_output
    )


def ensure_minikube_running() -> None:
    try:
        result = run(
            ["minikube", "status", "--format={{.Host}}"],
            capture_output=True
        )
        status = result.stdout.strip()
    except subprocess.CalledProcessError:
        status = "Stopped"

    if status != "Running":
        print("Starting Minikube...")
        run(["minikube", "start", "--driver=docker"])
    else:
        print("Minikube already running.")


def build_and_load_image(
        image_name: str,
        service_dir: Path,
        no_cache: bool = False
) -> None:
    build_cmd = ["docker", "build", "-t", image_name]

    if no_cache:
        build_cmd.append("--no-cache")

    build_cmd.append(str(service_dir))

    print(f"Building image: {image_name}")
    run(build_cmd)

    print(f"Loading image into Minikube: {image_name}")
    run(["minikube", "image", "load", image_name])


def apply_manifests(manifests: List[str]) -> None:
    for manifest in manifests:
        path = K8S_DIR / manifest
        if path.exists():
            run(["kubectl", "apply", "-f", str(path)])
        else:
            print(f"Warning: {path} not found. Skipping.")


def delete_deployment(name: str) -> None:
    print(f"Deleting deployment: {name}")
    run([
        "kubectl", "delete", "deployment", name,
        "-n", NAMESPACE,
        "--ignore-not-found"
    ])


def rollout_and_wait(deployment_name: str, timeout: str = "120s") -> None:
    print(f"Waiting for rollout: {deployment_name}")
    run([
        "kubectl", "rollout", "status",
        f"deployment/{deployment_name}",
        "-n", NAMESPACE,
        f"--timeout={timeout}"
    ])


def restart_deployment(name: str) -> None:
    run([
        "kubectl", "rollout", "restart",
        "deployment", name,
        "-n", NAMESPACE
    ])


def deploy_service(
        name: str,
        image: str,
        service_dir: Path,
        manifests: List[str],
        no_cache: bool = False,
        fresh: bool = False
) -> None:
    print(f"\n=== Deploying {name} ===")

    build_and_load_image(image, service_dir, no_cache=no_cache)

    if fresh:
        delete_deployment(name)

    apply_manifests(manifests)

    if not fresh:
        restart_deployment(name)

    rollout_and_wait(name)

    print(f"{name} deployed successfully.\n")


def deploy_ingress() -> None:
    ingress_path = K8S_DIR / "ingress.yaml"
    if ingress_path.exists():
        print("\n=== Applying Ingress ===")
        run(["kubectl", "apply", "-f", str(ingress_path)])
        print("Ingress applied.\n")
    else:
        print(f"Warning: {ingress_path} not found. Skipping ingress.")


def parse_args():
    parser = argparse.ArgumentParser(
        description="Deploy services to Minikube."
    )

    parser.add_argument(
        "--no-cache",
        action="store_true",
        help="Rebuild Docker images without cache."
    )

    parser.add_argument(
        "--fresh",
        action="store_true",
        help="Delete deployments before applying manifests."
    )

    return parser.parse_args()


def main() -> None:
    args = parse_args()
    ensure_minikube_running()
    deploy_service(
        name="auth-app",
        image="auth-app:latest",
        service_dir=Path("../auth_service"),
        manifests=["secrets.yaml", "auth.yaml"],
        no_cache=args.no_cache,
        fresh=args.fresh,
    )
    deploy_service(
        name="game-app",
        image="game-app:latest",
        service_dir=Path("../game_service"),
        manifests=["secrets.yaml", "game.yaml"],
        no_cache=args.no_cache,
        fresh=args.fresh,
    )
    deploy_ingress()


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as e:
        print(f"\nCommand failed: {' '.join(e.cmd)}")
        sys.exit(1)
