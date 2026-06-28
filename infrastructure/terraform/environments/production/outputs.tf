output "vpc_id" {
  value = module.vpc.vpc_id
}

output "ecr_repository_urls" {
  value = module.ecr.repository_urls
}

output "rds_endpoint" {
  value     = module.rds.endpoint
  sensitive = true
}

output "rds_jdbc_url" {
  value     = module.rds.jdbc_url
  sensitive = true
}

output "alb_dns_name" {
  description = "ALB DNS — point Cloudflare CNAME here"
  value       = module.alb.alb_dns_name
}

output "uploads_bucket" {
  value = module.s3.uploads_bucket_id
}

output "eks_cluster_name" {
  value = module.eks.cluster_name
}

output "eks_endpoint" {
  value = module.eks.cluster_endpoint
}


output "chatbot_endpoint_name" {
  description = "SageMaker chatbot endpoint name"
  value       = module.sagemaker.endpoint_name
}

output "chatbot_endpoint_url" {
  description = "SageMaker chatbot invocation URL"
  value       = module.sagemaker.endpoint_url
}
