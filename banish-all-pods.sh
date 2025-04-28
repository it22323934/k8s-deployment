#!/bin/bash

# Delete all resources in restaurant-system namespace
echo "Deleting restaurant-system resources..."
kubectl delete all --all -n restaurant-system

# Delete all resources in user-system namespace
echo "Deleting user-system resources..."
kubectl delete all --all -n user-system

# Delete the namespaces
echo "Deleting namespaces..."
kubectl delete namespace restaurant-system
kubectl delete namespace user-system

echo "All resources deleted successfully"