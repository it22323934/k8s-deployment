#!/bin/bash

  # Apply restaurant infrastructure
echo "Deploying restaurant infrastructure..."
kubectl apply -f k8s/manifests/infrastructure/restaurant-service/restaurant_mongo.yaml
kubectl apply -f k8s/manifests/infrastructure/restaurant-service/restaurant_zookeeper.yaml
kubectl apply -f k8s/manifests/infrastructure/restaurant-service/restaurant_broker.yaml
kubectl apply -f k8s/manifests/infrastructure/restaurant-service/restaurant_schema_registry.yaml
kubectl apply -f k8s/manifests/infrastructure/restaurant-service/restaurant_kafka_ui.yaml

  # Apply user infrastructure
echo "Deploying user infrastructure..."
kubectl apply -f k8s/manifests/infrastructure/user-service/user_mysql.yaml
kubectl apply -f k8s/manifests/infrastructure/user-service/user_zookeeper.yaml
kubectl apply -f k8s/manifests/infrastructure/user-service/user_broker.yaml
kubectl apply -f k8s/manifests/infrastructure/user-service/user_schema_registry.yaml
kubectl apply -f k8s/manifests/infrastructure/user-service/user_kafka_ui.yaml

  # Wait for pods to be ready
echo "Waiting for pods to be ready..."
kubectl wait --namespace=user-system --for=condition=ready pod --all --timeout=120s

echo "All components deployed successfully"