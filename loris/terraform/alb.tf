module "loris_alb" {
  source  = "../../terraform/ecs_alb"
  name    = "loris"
  subnets = ["${data.terraform_remote_state.platform.vpc_api_subnets}"]

  loadbalancer_security_groups = [
    "${module.loris_cluster_asg.loadbalancer_sg_https_id}",
    "${module.loris_cluster_asg.loadbalancer_sg_http_id}",
  ]

  certificate_domain = "api.wellcomecollection.org"
  vpc_id             = "${data.terraform_remote_state.platform.vpc_api_id}"

  alb_access_log_bucket = "${data.terraform_remote_state.platform.bucket_alb_logs_id}"
}
module "loris_alb_ebs" {
  source  = "../../terraform/ecs_alb"
  name    = "loris-ebs"
  subnets = ["${data.terraform_remote_state.platform.vpc_api_subnets}"]

  loadbalancer_security_groups = [
    "${module.loris_cluster_asg_ebs.loadbalancer_sg_https_id}",
    "${module.loris_cluster_asg_ebs.loadbalancer_sg_http_id}",
  ]

  certificate_domain = "api.wellcomecollection.org"
  vpc_id             = "${data.terraform_remote_state.platform.vpc_api_id}"

  alb_access_log_bucket = "${data.terraform_remote_state.platform.bucket_alb_logs_id}"
}
