output "zone_id" {
  description = "Route53 hosted zone ID"
  value       = aws_route53_zone.main.zone_id
}

output "name_servers" {
  description = "Name servers to configure in Cloudflare Registrar"
  value       = aws_route53_zone.main.name_servers
}

output "domain_name" {
  value = var.domain_name
}
