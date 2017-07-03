data "aws_acm_certificate" "certificate" {
  domain   = "${var.certificate_domain}"
  statuses = ["ISSUED"]
}
