module "api-stage" {
  source = "api.wellcomecollection.org"

  subdomain   = "api-stage"
  cert_domain = "api"

  public_api_bucket_domain_name = "${aws_s3_bucket.public_api.bucket_domain_name}"

  description = "Collection APIs staging"

  cf_logging_bucket = "${aws_s3_bucket.api_root_cf_logs.bucket_domain_name}"
}

module "api" {
  source = "api.wellcomecollection.org"

  subdomain   = "api"
  cert_domain = "api"

  public_api_bucket_domain_name = "${aws_s3_bucket.public_api.bucket_domain_name}"

  description = "Collection APIs production"

  cf_logging_bucket = "${aws_s3_bucket.api_root_cf_logs.bucket_domain_name}"
}

// S3 origin for redirect to developers.wellcomecollection.org
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
