#!/bin/bash

# GitHub Container Registry Configuration
GITHUB_USERNAME="${GITHUB_USERNAME:-your-github-username}"
REGISTRY="ghcr.io"
IMAGE_NAME="source-data-binance"
VERSION="1"
FULL_IMAGE_NAME="${REGISTRY}/${GITHUB_USERNAME}/${IMAGE_NAME}:${VERSION}"
LATEST_TAG="${REGISTRY}/${GITHUB_USERNAME}/${IMAGE_NAME}:latest"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_message() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if GITHUB_USERNAME is set
if [ "$GITHUB_USERNAME" = "your-github-username" ]; then
    print_warning "GITHUB_USERNAME environment variable is not set."
    read -p "Enter your GitHub username: " GITHUB_USERNAME
    if [ -z "$GITHUB_USERNAME" ]; then
        print_error "GitHub username cannot be empty."
        exit 1
    fi
    FULL_IMAGE_NAME="${REGISTRY}/${GITHUB_USERNAME}/${IMAGE_NAME}:${VERSION}"
    LATEST_TAG="${REGISTRY}/${GITHUB_USERNAME}/${IMAGE_NAME}:latest"
fi

print_message "Building Docker image: ${FULL_IMAGE_NAME}"

# Build the Docker image
if docker build -t "${FULL_IMAGE_NAME}" -t "${LATEST_TAG}" .; then
    print_message "Docker image built successfully"
else
    print_error "Failed to build Docker image"
    exit 1
fi

# Login to GitHub Container Registry
print_message "Logging in to GitHub Container Registry..."
print_message "Please enter your GitHub Personal Access Token (PAT) when prompted for password"
if ! echo "$GITHUB_TOKEN" | docker login ghcr.io -u "${GITHUB_USERNAME}" --password-stdin 2>/dev/null; then
    print_warning "Environment variable GITHUB_TOKEN not set or invalid. Please login manually."
    if ! docker login ghcr.io -u "${GITHUB_USERNAME}"; then
        print_error "Failed to login to GitHub Container Registry"
        exit 1
    fi
fi

# Push version tag
print_message "Pushing ${FULL_IMAGE_NAME} to GitHub Container Registry..."
if docker push "${FULL_IMAGE_NAME}"; then
    print_message "Successfully pushed ${FULL_IMAGE_NAME}"
else
    print_error "Failed to push ${FULL_IMAGE_NAME}"
    exit 1
fi

# Push latest tag
print_message "Pushing ${LATEST_TAG} to GitHub Container Registry..."
if docker push "${LATEST_TAG}"; then
    print_message "Successfully pushed ${LATEST_TAG}"
else
    print_error "Failed to push ${LATEST_TAG}"
    exit 1
fi

print_message "========================================="
print_message "Docker image published successfully!"
print_message "Image: ${FULL_IMAGE_NAME}"
print_message "Latest: ${LATEST_TAG}"
print_message "========================================="
print_message ""
print_message "To pull this image, run:"
print_message "  docker pull ${FULL_IMAGE_NAME}"
print_message "  or"
print_message "  docker pull ${LATEST_TAG}"
print_message ""
print_message "To run this image, use:"
print_message "  docker run -p 11080:11080 --name trading-demo ${FULL_IMAGE_NAME}"