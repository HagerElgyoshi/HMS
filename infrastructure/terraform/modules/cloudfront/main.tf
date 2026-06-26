# =============================================================================
#  CloudFront Module — CDN for sofcore-hms.com
#  Origin: Application Load Balancer
# =============================================================================

resource "aws_cloudfront_distribution" "main" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "HMS Production CDN"
  default_root_object = ""
  http_version        = "http2and3"
  price_class         = "PriceClass_100"

  aliases = [var.domain_name, "www.${var.domain_name}"]

  # ── Origin: ALB ──────────────────────────────────────────────────────────
  origin {
    domain_name = var.alb_dns_name
    origin_id   = "alb-origin"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  # ── Default behavior (frontend SPA) ─────────────────────────────────────
  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "alb-origin"
    viewer_protocol_policy = "redirect-to-https"
    compress               = true

    cache_policy_id          = aws_cloudfront_cache_policy.static.id
    origin_request_policy_id = aws_cloudfront_origin_request_policy.forward_host.id

    response_headers_policy_id = aws_cloudfront_response_headers_policy.security.id
  }

  # ── API behavior (no caching, forward everything) ───────────────────────
  ordered_cache_behavior {
    path_pattern           = "/api/*"
    allowed_methods        = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "alb-origin"
    viewer_protocol_policy = "redirect-to-https"
    compress               = true

    cache_policy_id          = aws_cloudfront_cache_policy.disabled.id
    origin_request_policy_id = aws_cloudfront_origin_request_policy.all_viewer.id
  }

  # ── TLS ─────────────────────────────────────────────────────────────────
  viewer_certificate {
    acm_certificate_arn      = var.certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  # ── Geo restriction ─────────────────────────────────────────────────────
  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  # ── WAF ─────────────────────────────────────────────────────────────────
  web_acl_id = var.waf_acl_arn

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-cdn"
  })
}

# ── Cache Policies ────────────────────────────────────────────────────────────
resource "aws_cloudfront_cache_policy" "static" {
  name        = "${var.project_name}-static-cache"
  default_ttl = 86400
  max_ttl     = 31536000
  min_ttl     = 0

  parameters_in_cache_key_and_forwarded_to_origin {
    cookies_config { cookie_behavior = "none" }
    headers_config { header_behavior = "none" }
    query_strings_config { query_string_behavior = "none" }
    enable_accept_encoding_brotli = true
    enable_accept_encoding_gzip   = true
  }
}

resource "aws_cloudfront_cache_policy" "disabled" {
  name        = "${var.project_name}-no-cache"
  default_ttl = 0
  max_ttl     = 0
  min_ttl     = 0

  parameters_in_cache_key_and_forwarded_to_origin {
    cookies_config { cookie_behavior = "all" }
    headers_config { header_behavior = "whitelist"
      headers { items = ["Authorization", "Origin", "Accept"] }
    }
    query_strings_config { query_string_behavior = "all" }
  }
}

# ── Origin Request Policies ──────────────────────────────────────────────────
resource "aws_cloudfront_origin_request_policy" "forward_host" {
  name = "${var.project_name}-forward-host"
  cookies_config { cookie_behavior = "none" }
  headers_config { header_behavior = "whitelist"
    headers { items = ["Host", "Origin"] }
  }
  query_strings_config { query_string_behavior = "none" }
}

resource "aws_cloudfront_origin_request_policy" "all_viewer" {
  name = "${var.project_name}-all-viewer"
  cookies_config { cookie_behavior = "all" }
  headers_config { header_behavior = "allViewer" }
  query_strings_config { query_string_behavior = "all" }
}

# ── Response Headers Policy (security) ───────────────────────────────────────
resource "aws_cloudfront_response_headers_policy" "security" {
  name = "${var.project_name}-security-headers"

  security_headers_config {
    strict_transport_security {
      access_control_max_age_sec = 31536000
      include_subdomains         = true
      preload                    = true
      override                   = true
    }
    content_type_options { override = true }
    frame_options { frame_option = "SAMEORIGIN"; override = true }
    xss_protection { mode_block = true; protection = true; override = true }
    referrer_policy { referrer_policy = "strict-origin-when-cross-origin"; override = true }
  }
}
