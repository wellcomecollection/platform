locals {
  pl-winslow-service  = "com.amazonaws.vpce.eu-west-1.vpce-svc-06514e3abdfb8f65e"
  wt-winnipeg-service = "com.amazonaws.vpce.eu-west-1.vpce-svc-0e3a1d25e0a776164"

  eu-west-1a-subnet = "subnet-0e2c16ae42b5d142f"
}

resource "aws_route53_zone" "internal" {
  name   = "storage.internal."
  vpc_id = "${local.vpc_id}"
}

# pl-winslow

resource "aws_vpc_endpoint" "pl-winslow" {
  vpc_id            = "${local.vpc_id}"
  service_name      = "${local.pl-winslow-service}"
  vpc_endpoint_type = "Interface"

  security_group_ids = [
    "${aws_security_group.interservice.id}",
  ]

  subnet_ids          = ["${local.eu-west-1a-subnet}"]
  private_dns_enabled = false
}

resource "aws_route53_record" "pl-winslow" {
  zone_id = "${aws_route53_zone.internal.zone_id}"
  name    = "pl-winslow.${aws_route53_zone.internal.name}"
  type    = "CNAME"
  ttl     = "300"
  records = ["${lookup(aws_vpc_endpoint.pl-winslow.dns_entry[0], "dns_name")}"]
}

# wt-winnipeg

resource "aws_vpc_endpoint" "wt-winnipeg" {
  vpc_id            = "${local.vpc_id}"
  service_name      = "${local.wt-winnipeg-service}"
  vpc_endpoint_type = "Interface"

  security_group_ids = [
    "${aws_security_group.interservice.id}",
  ]

  subnet_ids          = ["${local.eu-west-1a-subnet}"]
  private_dns_enabled = false
}

resource "aws_route53_record" "wt-winnipeg" {
  zone_id = "${aws_route53_zone.internal.zone_id}"
  name    = "wt-winnipeg.${aws_route53_zone.internal.name}"
  type    = "CNAME"
  ttl     = "300"
  records = ["${lookup(aws_vpc_endpoint.wt-winnipeg.dns_entry[0], "dns_name")}"]
}
