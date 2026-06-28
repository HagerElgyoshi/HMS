# =============================================================================
#  Production Environment — Module Composition
#  Architecture: AWS (compute/data) + Cloudflare (DNS/SSL/CDN/proxy)
#  NO Route53, NO CloudFront, NO ACM — Cloudflare Free handles edge.
# =============================================================================

locals {
  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

# ── VPC ───────────────────────────────────────────────────────────────────────
module "vpc" {
  source = "../../modules/vpc"

  vpc_cidr     = var.vpc_cidr
  project_name = var.project_name
  environment  = var.environment
  tags         = local.common_tags
}

# ── Security Groups ──────────────────────────────────────────────────────────
module "security" {
  source = "../../modules/security"

  vpc_id       = module.vpc.vpc_id
  project_name = var.project_name
  environment  = var.environment
  tags         = local.common_tags
}

# ── S3 Buckets ───────────────────────────────────────────────────────────────
module "s3" {
  source = "../../modules/s3"

  project_name = var.project_name
  environment  = var.environment
  tags         = local.common_tags
}

# ── IAM Roles & Policies ────────────────────────────────────────────────────
module "iam" {
  source = "../../modules/iam"

  project_name  = var.project_name
  environment   = var.environment
  aws_region    = var.aws_region
  s3_bucket_arn = module.s3.uploads_bucket_arn
  tags          = local.common_tags
}

# ── ECR Repositories ─────────────────────────────────────────────────────────
module "ecr" {
  source = "../../modules/ecr"

  project_name     = var.project_name
  repository_names = ["backend", "frontend"]
  tags             = local.common_tags
}

# ── RDS (MySQL) ──────────────────────────────────────────────────────────────
module "rds" {
  source = "../../modules/rds"

  project_name         = var.project_name
  environment          = var.environment
  database_username    = var.database_username
  database_password    = var.database_password
  instance_class       = var.rds_instance_class
  multi_az             = var.rds_multi_az
  db_subnet_group_name = module.vpc.db_subnet_group_name
  security_group_id    = module.security.database_security_group_id
  monitoring_role_arn  = ""
  tags                 = local.common_tags
}

# ── Application Load Balancer ────────────────────────────────────────────────
module "alb" {
  source = "../../modules/alb"

  project_name      = var.project_name
  environment       = var.environment
  vpc_id            = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnet_ids
  security_group_id = module.security.alb_security_group_id
  tags              = local.common_tags
}

# ── EKS Cluster ──────────────────────────────────────────────────────────────
module "eks" {
  source = "../../modules/eks"

  project_name       = var.project_name
  environment        = var.environment
  private_subnet_ids = module.vpc.private_subnet_ids
  cluster_role_arn   = module.iam.eks_cluster_role_arn
  node_role_arn      = module.iam.eks_node_role_arn
  instance_types     = ["t3.medium"]
  desired_nodes      = 2
  min_nodes          = 1
  max_nodes          = 3
  tags               = local.common_tags
}


# ── SageMaker (AI Chatbot — HakimAI) ────────────────────────────────────────
# IMPORTANT: Build and push the chatbot Docker image to ECR BEFORE applying.
# Command: docker build -f ai_chatbot/Dockerfile.sagemaker -t hms/chatbot ai_chatbot/
#          docker push <ECR_URL>/hms/chatbot:v1.0.0
#
# Then set: var.chatbot_image_uri = "<ECR_URL>/hms/chatbot:v1.0.0"
module "sagemaker" {
  source = "../../modules/sagemaker"

  project_name      = var.project_name
  environment       = var.environment
  chatbot_image_uri = var.chatbot_image_uri
  instance_type     = "ml.g4dn.xlarge"
  instance_count    = 1
  enable_autoscaling = false
  tags              = local.common_tags
}
