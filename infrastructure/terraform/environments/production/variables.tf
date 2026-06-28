variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project identifier"
  type        = string
  default     = "hms"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "production"
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
  default     = "10.0.0.0/16"
}

variable "database_username" {
  description = "RDS master username"
  type        = string
  sensitive   = true
  default     = "hms_admin"
}

variable "database_password" {
  description = "RDS master password"
  type        = string
  sensitive   = true
  default     = "HmsPass2024Secure99"
}

variable "rds_instance_class" {
  description = "RDS instance type"
  type        = string
  default     = "db.t3.micro"
}

variable "rds_multi_az" {
  description = "Enable Multi-AZ"
  type        = bool
  default     = false
}


variable "chatbot_image_uri" {
  description = "ECR image URI for the AI chatbot (HakimAI)"
  type        = string
  default     = "529088275461.dkr.ecr.us-east-1.amazonaws.com/hms/chatbot:v1.0.0"
}
