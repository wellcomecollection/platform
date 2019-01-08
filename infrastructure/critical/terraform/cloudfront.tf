data "aws_acm_certificate" "api_wc_org" {
  domain   = "api.wellcomecollection.org"
  statuses = ["ISSUED"]
  provider = "aws.us-east-1"
}

locals {
  catalogue_domain_name = "catalogue.api.wellcomecollection.org"
}

resource "aws_cloudfront_distribution" "api_root" {

  // root

  origin {
    domain_name = "${aws_s3_bucket.public_api.bucket_domain_name}"
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
      origin_ssl_protocols   = [
        "TLSv1", "TLSv1.1", "TLSv1.2", "SSLv3"
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

      headers = ["*"]

      cookies {
        forward = "all"
      }
    }

    compress               = true
    viewer_protocol_policy = "redirect-to-https"

    min_ttl     = 0
    default_ttl = 0
    max_ttl     = 0
  }

  // shared config

  enabled         = true
  is_ipv6_enabled = true

  default_root_object = "index.html"

  aliases = [
    "api.wellcomecollection.org",
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

// Root redirect to developers.wc

resource "aws_s3_bucket" "public_api" {
  bucket = "wellcomecollection-public-api"
  acl    = "public-read"

  website {
    index_document = "index.html"
  }

  policy = <<EOF
{
  "Version": "2008-10-17",
  "Statement": [
    {
      "Sid": "PublicReadForGetBucketObjects",
      "Effect": "Allow",
      "Principal": {
        "AWS": "*"
      },
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::wellcomecollection-public-api/*"
    }
  ]
}
EOF
}

resource "aws_s3_bucket_object" "object" {
  bucket       = "${aws_s3_bucket.public_api.bucket}"
  key          = "index.html"
  source       = "${path.module}/s3_objects/index.html"
  etag         = "${md5(file("${path.module}/s3_objects/index.html"))}"
  content_type = "text/html"
}
