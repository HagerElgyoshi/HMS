variable "project_name" { type = string }
variable "environment" { type = string }
variable "domain_name" { type = string; default = "sofcore-hms.com" }
variable "alb_dns_name" { type = string }
variable "certificate_arn" { type = string; description = "ACM cert ARN (us-east-1 for CloudFront)" }
variable "waf_acl_arn" { type = string; default = "" }
variable "tags" { type = map(string); default = {} }
