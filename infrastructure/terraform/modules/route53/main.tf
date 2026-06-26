# =============================================================================
#  Route53 Module — DNS Management for sofcore-hms.com
#  Cloudflare is ONLY the registrar. Route53 is the authoritative DNS.
# =============================================================================

resource "aws_route53_zone" "main" {
  name    = var.domain_name
  comment = "Managed by Terraform — HMS Production"

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-zone"
  })
}

# ── Root domain → ALB ─────────────────────────────────────────────────────────
resource "aws_route53_record" "root" {
  zone_id = aws_route53_zone.main.zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = var.cloudfront_domain_name
    zone_id                = var.cloudfront_hosted_zone_id
    evaluate_target_health = true
  }
}

# ── www → CloudFront ──────────────────────────────────────────────────────────
resource "aws_route53_record" "www" {
  zone_id = aws_route53_zone.main.zone_id
  name    = "www.${var.domain_name}"
  type    = "A"

  alias {
    name                   = var.cloudfront_domain_name
    zone_id                = var.cloudfront_hosted_zone_id
    evaluate_target_health = true
  }
}

# ── api → ALB directly ────────────────────────────────────────────────────────
resource "aws_route53_record" "api" {
  zone_id = aws_route53_zone.main.zone_id
  name    = "api.${var.domain_name}"
  type    = "A"

  alias {
    name                   = var.alb_dns_name
    zone_id                = var.alb_zone_id
    evaluate_target_health = true
  }
}

# ── Monitoring subdomains (optional) ──────────────────────────────────────────
resource "aws_route53_record" "grafana" {
  count   = var.enable_monitoring_records ? 1 : 0
  zone_id = aws_route53_zone.main.zone_id
  name    = "grafana.${var.domain_name}"
  type    = "A"

  alias {
    name                   = var.alb_dns_name
    zone_id                = var.alb_zone_id
    evaluate_target_health = true
  }
}
