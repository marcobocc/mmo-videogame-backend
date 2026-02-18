#!/usr/bin/env python3

import argparse
import subprocess
import sys
from pathlib import Path
from typing import List, Optional

MINIKUBE_PROFILE = "mmo-cluster"

class MinikubeDeployer:
    def __init__(self, root_dir: Path, profile: str = MINIKUBE_PROFILE):
        self.root_dir = root_dir.resolve()
        self.k8s_dir = self.root_dir / "infra/k8s"
        self.profile = profile
        self.services = {
            "auth": {
                "image": "auth-app:latest",
                "service_dir": self.root_dir / "auth_service",
                "manifests": ["secrets.yaml", "auth.yaml"]
            },
            "game": {
                "image": "game-app:latest",
                "service_dir": self.root_dir / "game_service",
                "manifests": ["secrets.yaml", "game.yaml"]
            }
        }

    @staticmethod
    def run(cmd: List[str], capture_output: bool = False) -> subprocess.CompletedProcess:
        print(f"$ {' '.join(cmd)}")
        return subprocess.run(cmd, check=True, text=True, capture_output=capture_output)

    # ----------------- Minikube management -----------------
    def ensure_minikube_running(self) -> None:
        try:
            status = self.run(
                ["minikube", "status", "--format={{.Host}}", f"--profile={self.profile}"],
                capture_output=True
            ).stdout.strip()
        except subprocess.CalledProcessError:
            status = "Stopped"

        if status != "Running":
            print(f"Starting Minikube ({self.profile})...")
            self.run(["minikube", "start", "--driver=docker", f"--profile={self.profile}"])
        else:
            print(f"Minikube ({self.profile}) already running.")

    def ensure_minikube_addons(self):
        print("\n=== Ensuring Minikube Ingress Controller ===")
        addons = self.run(
            ["minikube", "addons", "list", f"--profile={self.profile}"],
            capture_output=True
        ).stdout
        if "ingress: enabled" not in addons:
            print("Enabling ingress addon...")
            self.run(["minikube", "addons", "enable", "ingress", f"--profile={self.profile}"])

    # ----------------- Docker image management -----------------
    def build_and_load_image(self, image_name: str, service_dir: Path) -> None:
        print(f"Building Docker image: {image_name}")
        self.run(["docker", "build", "-t", image_name, str(service_dir)])
        print(f"Loading image into Minikube: {image_name}")
        self.run(["minikube", "image", "load", image_name, f"--profile={self.profile}"])

    # ----------------- Kubernetes manifests -----------------
    def apply_manifests(self, manifests: List[str]) -> None:
        for manifest in manifests:
            path = self.k8s_dir / manifest
            if path.exists():
                self.run(["kubectl", "apply", "-f", str(path)])
            else:
                print(f"Warning: {path} not found. Skipping.")

    def rollout_and_wait(self, name: str, timeout: str = "180s") -> None:
        for kind in ["deployment", "statefulset"]:
            try:
                self.run([
                    "kubectl", "rollout", "status",
                    f"{kind}/{name}",
                    f"--timeout={timeout}"
                ])
                return
            except subprocess.CalledProcessError:
                continue
        print(f"No rollout resource found for {name}, skipping wait.")

    # ----------------- Service deployment -----------------
    def deploy_service(self, service_name: str) -> None:
        if service_name not in self.services:
            print(f"Unknown service '{service_name}', skipping.")
            return

        svc = self.services[service_name]
        print(f"\n=== Deploying {service_name} ===")
        self.build_and_load_image(svc["image"], svc["service_dir"])
        self.apply_manifests(svc["manifests"])
        for workload in [f"{service_name}-app", f"{service_name}-postgres"]:
            self.rollout_and_wait(workload)

        print(f"{service_name} deployed successfully.\n")

    # ----------------- Ingress -----------------
    def deploy_ingress(self) -> None:
        ingress_path = self.k8s_dir / "ingress.yaml"
        if ingress_path.exists():
            print("Applying ingress manifest...")
            self.run([
                "minikube", "kubectl", "--profile", self.profile, "--",
                "apply", "-f", str(ingress_path)
            ])
            command_hint = "kubectl port-forward --namespace ingress-nginx svc/ingress-nginx-controller 8080:80 8443:443"
            print(f"Ingress applied. Remember to run '{command_hint}' to access services via localhost.")
        else:
            print(f"No ingress.yaml found at {ingress_path}, skipping.")

    # ----------------- Full deployment -----------------
    def deploy(self, service: Optional[str] = None) -> None:
        self.ensure_minikube_running()
        self.ensure_minikube_addons()
        if service:
            self.deploy_service(service)
        else:
            for svc_name in self.services.keys():
                self.deploy_service(svc_name)
            self.deploy_ingress()

    # ----------------- Teardown -----------------
    def teardown(self) -> None:
        print(f"Deleting Minikube cluster ({self.profile})...")
        self.run(["minikube", "delete", f"--profile={self.profile}"])


# ----------------- CLI -----------------
def parse_args():
    parser = argparse.ArgumentParser(description="Deploy services to Minikube.")
    parser.add_argument(
        "--service",
        type=str,
        choices=["auth", "game"],
        help="Deploy only a specific service"
    )
    parser.add_argument(
        "--teardown",
        action="store_true",
        help="Delete the custom Minikube cluster and all resources"
    )
    return parser.parse_args()

if __name__ == "__main__":
    try:
        args = parse_args()
        deployer = MinikubeDeployer(root_dir=Path.cwd())
        if args.teardown:
            deployer.teardown()
        else:
            deployer.deploy(service=args.service)
    except subprocess.CalledProcessError as e:
        print(f"\nCommand failed: {' '.join(e.cmd)}")
        sys.exit(1)
