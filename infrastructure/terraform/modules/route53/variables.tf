variable "domain_name" {
  description = "Production domain name"
  type        = string
  default     = "sofcore-hms.com"
}

variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "alb_dns_name" {
  description = "ALB DNS name for alias records"
  type        = string
}

variable "alb_zone_id" {
  description = "ALB hosted zone ID"
  type        = string
}

variable "cloudfront_domain_name" {
  description = "CloudFront distribution domain name"
  type        = string
}

variable "cloudfront_hosted_zone_id" {
  description = "CloudFront hosted zone ID (always Z2FDTNDATAQYW2)"
  type        = string
  default     = "Z2FDTNDATAQYW2"
}

variable "enable_monitoring_records" {
  description = "Create DNS records for grafana/prometheus subdomains"
  type        = bool
  default     = false
}

variable "tags" {
  type    = map(string)
  default = {}
}
