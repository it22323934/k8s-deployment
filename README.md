# Food Delivery Platform Deployment Guide

This README provides instructions for deploying the Food Delivery microservices platform on Kubernetes.

## Prerequisites

- Kubernetes cluster (minikube, kind, or cloud provider)
- kubectl CLI configured
- Docker (for local image building)
- git (to clone this repository)

## Repository Structure

```
├── k8s/
│   ├── manifests/
│   │   ├── infrastructure/
│   │   │   ├── order-service/
│   │   │   ├── restaurant-service/
│   │   │   └── user-service/
│   │   └── application/
├── scripts/
│   ├── infrastructure-create-pods.sh
│   └── application-create-pods.sh
```

## Deployment Steps

### 1. Clone the Repository

```bash
git clone <repository-url>
cd <repository-directory>
```

### 2. Deploy Infrastructure Components

Run the infrastructure deployment script to create all necessary infrastructure components (MongoDB, MySQL, Kafka, Zookeeper, etc.):

```bash
chmod +x scripts/infrastructure-create-pods.sh
./scripts/infrastructure-create-pods.sh
```

This script deploys:
- Restaurant service infrastructure (MongoDB, Zookeeper, Kafka, Schema Registry)
- User service infrastructure (MySQL, Zookeeper, Kafka, Schema Registry)
- Order service infrastructure (MongoDB)

### 3. Deploy Application Services

After the infrastructure is up and running, deploy the application services:

```bash
chmod +x scripts/application-create-pods.sh
./scripts/application-create-pods.sh
```

This script deploys:
- API Gateway
- Restaurant Service
- Order Service
- Notification Service

### 4. Verify Deployment

Check the status of all pods:

```bash
kubectl get pods -A
```

Verify the services are running:

```bash
kubectl get services
```

## Service Information

- **Order Service**: Manages customer orders
  - Port: 7083
  - Dependencies: Restaurant Service, MongoDB

- **Restaurant Service**: Manages restaurant details and menu items
  - Port: 8099
  - Dependencies: MongoDB, User Service

- **User Service**: Manages user authentication and profiles
  - Dependencies: MySQL

## Troubleshooting

If pods are not starting properly, check the logs:

```bash
kubectl logs <pod-name>
```

For infrastructure issues:

```bash
kubectl describe pod <infrastructure-pod-name>
```

## Cleanup

To remove all deployed resources:

```bash
kubectl delete -f k8s/manifests/application/
kubectl delete -f k8s/manifests/infrastructure/
```
