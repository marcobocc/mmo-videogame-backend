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
cd infra
python3 deploy.py
```

This script:

1. Starts Minikube if not running
2. Builds the authentication service Docker image
3. Loads the image into Minikube
4. Applies Kubernetes manifests
5. Restarts the deployment
6. Waits for rollout completion

### 3. Verify deployment

Check pods:

```bash
kubectl get pods
```

Check services:

```bash
kubectl get svc
```

Ensure:

* PostgreSQL pod is running
* Auth service pod is running
* No pods in `CrashLoopBackOff` or `Error` state

---

## Test the authentication service

In another terminal window, expose the auth service:

```bash
minikube service auth-app-svc --url
```

Example output:

```
http://127.0.0.1:30000
```

In the following commands, replace `<BASE_URL>` with the URL from Minikube above.

#### Registering new user:

```bash
curl -X POST <BASE_URL>/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"player1","password":"secret"}'
```

Expected response:

* `201 Created` if registration succeeds

#### Logging in as existing user:

```bash
curl -X POST <BASE_URL>/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"player1","password":"secret"}'
```

Expected response:

* `200 OK`
* JSON containing a JWT token:

```json
{
  "jwt": "<generated-token>"
}
```

---

## Debugging

### View logs

Auth service:

```bash
kubectl logs deployment/auth-app
```

PostgreSQL:

```bash
kubectl logs <postgres-pod-name>
```

### Describe pods

```bash
kubectl describe pod <pod-name>
```

Look for:

* Events showing container start failures
* Image pull errors
* Readiness probe failures

### Access pod shell

```bash
kubectl exec -it <pod-name> -- /bin/sh
```

Inspect files, environment variables, or network connectivity.

---

## Reset local environment

If the environment becomes inconsistent:

```bash
minikube delete
minikube start --driver=docker
```

Then redeploy:

```bash
cd infra
python3 deploy.py
```
