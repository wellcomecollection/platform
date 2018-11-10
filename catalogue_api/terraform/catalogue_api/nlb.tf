module "nlb" {
  source = "git::https://github.com/wellcometrust/terraform.git//load_balancer/network?ref=cfbc6c413003f953768e2ff97f47fad3f1f68ea5"

  namespace       = "${var.namespace}"
  private_subnets = ["${var.subnets}"]
}