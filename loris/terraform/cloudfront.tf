resource "aws_cloudfront_distribution" "loris" {
  origin {
    domain_name = "iiif-origin.wellcomecollection.org"
    origin_id   = "loris"

    custom_origin_config {
      https_port             = 443
      http_port              = 80
      origin_protocol_policy = "match-viewer"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  enabled         = true
  is_ipv6_enabled = true

  aliases = ["iiif.wellcomecollection.org"]

  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "loris"

    forwarded_values {
      query_string = true

      cookies {
        forward = "none"
      }

      headers = []
    }

    viewer_protocol_policy = "redirect-to-https"

    min_ttl     = 7200
    default_ttl = 315576000
    max_ttl     = 315576000
  }

  price_class = "PriceClass_100"

  viewer_certificate {
    acm_certificate_arn      = "${var.iiif_acm_cert_arn}"
    minimum_protocol_version = "TLSv1"
    ssl_support_method       = "sni-only"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  logging_config {
    include_cookies = false
    bucket          = "${local.cloudfront_logs_bucket_domain_name}"
    prefix          = "loris"
  }
}
