data "aws_acm_certificate" "api_wc_org" {
  domain   = "${var.cert_domain}.wellcomecollection.org"
  statuses = ["ISSUED"]
  provider = "aws.us-east-1"
}

locals {
  catalogue_domain_name = "catalogue.${var.subdomain}.wellcomecollection.org"
  storage_domain_name   = "storage.${var.subdomain}.wellcomecollection.org"
}

resource "aws_cloudfront_distribution" "api_root" {
  // root

  origin {
    domain_name = "${var.public_api_bucket_domain_name}"
    origin_id   = "root"
  }

  default_cache_behavior {
    allowed_methods = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods  = ["GET", "HEAD"]

    target_origin_id = "root"

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }

    viewer_protocol_policy = "redirect-to-https"

    min_ttl     = 0
    default_ttl = 3600
    max_ttl     = 86400
  }

  // catalogue

  origin {
    domain_name = "${local.catalogue_domain_name}"
    origin_id   = "catalogue_api"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"

      origin_ssl_protocols = [
        "TLSv1",
        "TLSv1.1",
        "TLSv1.2",
      ]
    }
  }
  ordered_cache_behavior {
    path_pattern = "/catalogue/*"

    allowed_methods = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods  = ["GET", "HEAD"]

    target_origin_id = "catalogue_api"

    forwarded_values {
      query_string = true

      headers = ["Authorization"]

      cookies {
        forward = "all"
      }
    }

    viewer_protocol_policy = "https-only"

    min_ttl     = 0
    default_ttl = 0
    max_ttl     = 0
  }

  // storage

  origin {
    domain_name = "${local.storage_domain_name}"
    origin_id   = "storage_api"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"

      origin_ssl_protocols = [
        "TLSv1",
        "TLSv1.1",
        "TLSv1.2",
      ]
    }
  }
  ordered_cache_behavior {
    path_pattern = "/storage/*"

    allowed_methods = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods  = ["GET", "HEAD"]

    target_origin_id = "storage_api"

    forwarded_values {
      query_string = true

      headers = ["Authorization"]

      cookies {
        forward = "all"
      }
    }

    viewer_protocol_policy = "https-only"

    min_ttl     = 0
    default_ttl = 0
    max_ttl     = 0
  }

  // shared config

  comment             = "${var.description}"
  enabled             = true
  is_ipv6_enabled     = true
  default_root_object = "index.html"
  aliases = [
    "${var.subdomain}.wellcomecollection.org",
  ]
  price_class = "PriceClass_100"
  viewer_certificate {
    acm_certificate_arn      = "${data.aws_acm_certificate.api_wc_org.arn}"
    minimum_protocol_version = "TLSv1"
    ssl_support_method       = "sni-only"
  }
  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }
}
