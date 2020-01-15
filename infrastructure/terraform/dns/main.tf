data "aws_route53_zone" "weco_zone" {
  provider = "aws.routemaster"
  name     = "wellcomecollection.org."
}

locals {
  weco_hosted_zone_id = "${data.aws_route53_zone.weco_zone.id}"
}

# Third-party services
resource "aws_route53_record" "docs" {
  zone_id = "${local.weco_hosted_zone_id}"
  name    = "docs.wellcomecollection.org"
  type    = "CNAME"
  records = ["hosting.gitbook.com"]
  ttl     = "300"

  provider = "aws.routemaster"
}

# Redirects
module "www" {
  source  = "./modules/redirect"
  from    = "www.wellcomecollection.org"
  to      = "wellcomecollection.org"
  zone_id = "${local.weco_hosted_zone_id}"

  providers = {
    aws.dns_provider = "aws.routemaster"
  }
}

# We output this for other service to use
# We don't import it directly from the routemaster stack as we haven't actually provisioned that via terraform properly
output "weco_hosted_zone_id" {
  value = "${local.weco_hosted_zone_id}"
}
