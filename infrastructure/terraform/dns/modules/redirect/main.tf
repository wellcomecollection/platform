provider "aws" {
  alias = "dns_provider"
}

resource "aws_s3_bucket" "redirect" {
  bucket = "${var.from}"
  acl    = "private"

  website {
    redirect_all_requests_to = "${var.to}"
  }
}

resource "aws_route53_record" "redirect_domain" {
  name    = "${var.from}"
  zone_id = "${var.zone_id}"
  type    = "A"

  alias {
    name                   = "${aws_s3_bucket.redirect.website_domain}"
    zone_id                = "${aws_s3_bucket.redirect.hosted_zone_id}"
    evaluate_target_health = true
  }

  provider = "aws.routemaster"
}
