variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Deployment environment"
  type        = string
}

variable "chatbot_image_uri" {
  description = "Full ECR image URI for the chatbot container (e.g. 529088275461.dkr.ecr.us-east-1.amazonaws.com/hms/chatbot:v1.0.0)"
  type        = string
}

variable "instance_type" {
  description = "SageMaker instance type (must have GPU for LLM inference)"
  type        = string
  default     = "ml.g4dn.xlarge"
}

variable "instance_count" {
  description = "Initial number of instances"
  type        = number
  default     = 1
}

variable "enable_autoscaling" {
  description = "Enable auto-scaling (can scale to 0 when idle)"
  type        = bool
  default     = false
}

variable "min_instance_count" {
  description = "Minimum instances when auto-scaling (0 = scale to zero)"
  type        = number
  default     = 0
}

variable "max_instance_count" {
  description = "Maximum instances when auto-scaling"
  type        = number
  default     = 1
}

variable "tags" {
  description = "Common tags"
  type        = map(string)
  default     = {}
}
