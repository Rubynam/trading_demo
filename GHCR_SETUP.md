# GitHub Container Registry Setup Guide

This guide explains how to publish Docker images to GitHub Container Registry (GHCR) instead of Docker Hub.

## Prerequisites

1. A GitHub account
2. Docker installed and running
3. A GitHub Personal Access Token (PAT)

## Step 1: Create GitHub Personal Access Token

1. Go to GitHub.com and navigate to:
   - **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)**
2. Click **"Generate new token (classic)"**
3. Give your token a descriptive name (e.g., "Docker GHCR Access")
4. Select the following scopes:
   - ✅ `write:packages` - Upload packages to GitHub Package Registry
   - ✅ `read:packages` - Download packages from GitHub Package Registry
   - ✅ `delete:packages` (optional) - Delete packages from GitHub Package Registry
5. Click **"Generate token"**
6. **IMPORTANT**: Copy the token immediately (you won't be able to see it again!)

## Step 2: Authenticate with GitHub Container Registry

### Option A: Using Environment Variables (Recommended)

Export your GitHub credentials as environment variables:

```bash
export GITHUB_USERNAME="your-github-username"
export GITHUB_TOKEN="your_github_personal_access_token"
```

To make these persistent, add them to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.):

```bash
echo 'export GITHUB_USERNAME="your-github-username"' >> ~/.zshrc
echo 'export GITHUB_TOKEN="your_github_personal_access_token"' >> ~/.zshrc
source ~/.zshrc
```

### Option B: Manual Login

Login manually to GHCR:

```bash
echo "YOUR_GITHUB_TOKEN" | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
```

Or interactive login:

```bash
docker login ghcr.io -u YOUR_GITHUB_USERNAME
# When prompted for password, enter your Personal Access Token
```

## Step 3: Build and Push Image

Run the publish script:

```bash
./publish.sh
```

The script will:
1. Build the Docker image
2. Tag it with version and latest tags
3. Login to GHCR (using environment variables or interactive login)
4. Push both tags to GitHub Container Registry

## Step 4: Verify Publication

After successful push, you can verify the image:

1. Visit: `https://github.com/YOUR_USERNAME?tab=packages`
2. You should see `source-data-binance` package listed

## Step 5: Make Package Public (Optional)

By default, packages are private. To make it public:

1. Go to your package page: `https://github.com/users/YOUR_USERNAME/packages/container/source-data-binance`
2. Click **"Package settings"**
3. Scroll to **"Danger Zone"**
4. Click **"Change visibility"** → **"Public"**

## Pulling Images from GHCR

### For Public Images

```bash
docker pull ghcr.io/YOUR_USERNAME/source-data-binance:latest
docker pull ghcr.io/YOUR_USERNAME/source-data-binance:1
```

### For Private Images

First authenticate:

```bash
echo "YOUR_GITHUB_TOKEN" | docker login ghcr.io -u YOUR_USERNAME --password-stdin
```

Then pull:

```bash
docker pull ghcr.io/YOUR_USERNAME/source-data-binance:latest
```

## Using in Docker Compose or Kubernetes

Update image references in your deployment files:

**Docker Compose:**
```yaml
services:
  app:
    image: ghcr.io/YOUR_USERNAME/source-data-binance:latest
```

**Kubernetes:**
```yaml
spec:
  containers:
  - name: app
    image: ghcr.io/YOUR_USERNAME/source-data-binance:latest
```

For private images in Kubernetes, create an image pull secret:

```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=YOUR_USERNAME \
  --docker-password=YOUR_GITHUB_TOKEN \
  --docker-email=your-email@example.com
```

## Troubleshooting

### Authentication Failed

- Ensure your PAT has the correct scopes (`write:packages`, `read:packages`)
- Verify the token hasn't expired
- Check that you're using the token as the password, not your GitHub password

### Permission Denied

- Verify your GitHub username is correct
- Ensure the PAT has proper permissions
- Check if your organization requires SSO authorization for the token

### Image Not Found After Push

- Wait a few minutes for GitHub to index the package
- Check package visibility settings
- Verify the image name and tag are correct

## Environment Variables Summary

| Variable | Description | Example |
|----------|-------------|---------|
| `GITHUB_USERNAME` | Your GitHub username | `octocat` |
| `GITHUB_TOKEN` | GitHub Personal Access Token | `ghp_xxxxxxxxxxxx` |

## Additional Resources

- [GitHub Container Registry Documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Managing GitHub Packages](https://docs.github.com/en/packages/learn-github-packages)
- [Docker Login Documentation](https://docs.docker.com/engine/reference/commandline/login/)