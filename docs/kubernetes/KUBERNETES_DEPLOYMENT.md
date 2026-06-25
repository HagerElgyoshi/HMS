# Kubernetes Deployment Guide — HMS

## Prerequisites

- AWS CLI configured
- kubectl installed
- Helm 3 installed
- EKS cluster provisioned (Phase 4 Terraform)
- ECR images pushed

## Connect to cluster

```bash
aws eks update-kubeconfig --name hms-production --region us-east-1
kubectl get nodes  # verify connectivity
```

## Deploy (step by step)

### 1. Create namespace and policies

```bash
kubectl apply -f infrastructure/kubernetes/namespace/
kubectl apply -f infrastructure/kubernetes/network-policies/
```

### 2. Install Metrics Server (for HPA)

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

### 3. Install AWS Load Balancer Controller

```bash
helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=hms-production \
  --set serviceAccount.create=true \
  --set serviceAccount.annotations."eks\.amazonaws\.com/role-arn"=<LB_CONTROLLER_ROLE_ARN>
```

### 4. Deploy Backend

```bash
cd infrastructure/helm/backend
helm dependency update
helm install hms-backend . \
  -n hms-production \
  -f values-production.yaml \
  --set image.repository=<ECR_BACKEND_URL> \
  --set image.tag=<VERSION> \
  --set serviceAccount.annotations."eks\.amazonaws\.com/role-arn"=<BACKEND_IRSA_ARN> \
  --set env.secret.DATABASE_URL=<RDS_JDBC_URL> \
  --set env.secret.DATABASE_USERNAME=<DB_USER> \
  --set env.secret.DATABASE_PASSWORD=<DB_PASS> \
  --set env.secret.JWT_SECRET=<SECRET> \
  --set env.secret.JWT_ACCESS_TOKEN_EXPIRATION=900000 \
  --set env.secret.JWT_REFRESH_TOKEN_EXPIRATION=604800000 \
  --set env.secret.CORS_ALLOWED_ORIGINS=https://hms.example.com
```

### 5. Deploy Frontend

```bash
cd infrastructure/helm/frontend
helm dependency update
helm install hms-frontend . \
  -n hms-production \
  -f values-production.yaml \
  --set image.repository=<ECR_FRONTEND_URL> \
  --set image.tag=<VERSION>
```

### 6. Deploy Ingress

```bash
cd infrastructure/helm/ingress
helm install hms-ingress . \
  -n hms-production \
  --set host=hms.example.com \
  --set tls.certificateArn=<ACM_CERT_ARN>
```

## Verify

```bash
kubectl get pods -n hms-production
kubectl get svc -n hms-production
kubectl get ingress -n hms-production
kubectl get hpa -n hms-production

# Check pod health
kubectl describe pod -l app.kubernetes.io/component=backend -n hms-production

# Check logs
kubectl logs -l app.kubernetes.io/component=backend -n hms-production --tail=50
```

## Rollback

```bash
helm rollback hms-backend <REVISION> -n hms-production
helm rollback hms-frontend <REVISION> -n hms-production
```
