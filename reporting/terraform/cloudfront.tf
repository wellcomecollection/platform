data "aws_acm_certificate" "reporting_wc_org" {
  domain   = "reporting.wellcomecollection.org"
  statuses = ["ISSUED"]
  provider = "aws.us_east_1"
}

resource "aws_cloudfront_distribution" "reporting" {
  origin {
    domain_name = "${var.reporting_kibana_domain_name}"
    origin_id   = "reporting"

    custom_origin_config {
      https_port             = 9243
      http_port              = 9200
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2", "TLSv1", "TLSv1.1"]
    }
  }

  enabled         = true
  is_ipv6_enabled = true

  aliases = ["reporting.wellcomecollection.org"]

  default_cache_behavior {
    allowed_methods = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]

    # This field apparently must be non-empty
    # OPTIONS is a fairly safe field to cache
    cached_methods = ["GET", "HEAD"]

    target_origin_id = "reporting"

    forwarded_values {
      query_string = true

      cookies {
        forward = "all"
      }
    }

    default_ttl = 0
    max_ttl     = 0

    viewer_protocol_policy = "redirect-to-https"
  }

  price_class = "PriceClass_100"

  viewer_certificate {
    acm_certificate_arn      = "${data.aws_acm_certificate.reporting_wc_org.arn}"
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
    prefix          = "reporting"
  }
}
