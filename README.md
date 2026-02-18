# MMO Backend – Local Setup, Testing & Troubleshooting

This document provides instructions for setting up, testing, and debugging the MMO backend locally.

---
## Local deployment

### 1. Check prerequisites

Local testing requires Docker, Minikube, kubectl and Python3 to be installed. You can verify that these are installed on your machine by running:

```bash
docker --version
minikube version
kubectl version --client
python3 --version
````

### 2. Deploy the application stack

Kubernetes manifest files and deployment scripts are located in the `infra/` folder. You can conveniently (re-)deploy the entire application stack by running:

```bash
python3 infra/deployment/minikube.py

Optional args:
    --service     # Deploys only specific backend service (e.g., auth, game)
    --teardown    # Deletes existing cluster (does not automatically redeploy)
```

This script:

1. Starts Minikube if not running
2. Builds the authentication service Docker image
3. Loads the image into Minikube
4. Applies Kubernetes manifests
5. Restarts the deployment
6. Waits for rollout completion

### 3. Verify deployment

Check pods and services:

```bash
kubectl get pods
kubectl get svc
```

Ensure that no pods are in `CrashLoopBackOff` or `Error` state.

Check ingress:

```bash
kubectl get svc --namespace ingress-nginx
```

Expected output:

```pgsql
NAME                                 TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                      AGE
ingress-nginx-controller             NodePort    10.96.144.125   <none>        80:31173/TCP,443:32571/TCP   16m
ingress-nginx-controller-admission   ClusterIP   10.104.8.247    <none>        443/TCP                      16m
```

---
## Sending requests
### 1. Forward ports

```bash
kubectl port-forward --namespace ingress-nginx svc/ingress-nginx-controller 8080:80 8443:443
```
Keep this running in another terminal while testing.

### 2. Test the authentication service

**Registering new user:**

```bash
curl http://127.0.0.1:8080/auth/register \
  -H "Host: mmo.local" \
  -H "Content-Type: application/json" \
  -d '{"username":"player1","password":"secret"}'

```

Expected response:

* `201 Created` if registration succeeds

**Logging in as existing user:**

```bash
curl http://127.0.0.1:8080/auth/login \
  -H "Host: mmo.local" \
  -H "Content-Type: application/json" \
  -d '{"username":"player1","password":"secret"}'
```

Expected response:

* `200 OK`
* JSON containing a JWT token

---

## Debugging
**View logs:**

```bash
kubectl logs deployment/auth-app
```

**Describe pods:**

```bash
kubectl describe pod <pod-name>
```

**Access pod shell:**

```bash
kubectl exec -it <pod-name> -- /bin/sh
```
