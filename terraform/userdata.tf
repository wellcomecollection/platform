module "services_userdata" {
  source       = "./userdata"
  cluster_name = "${aws_ecs_cluster.services.name}"
}

module "api_userdata" {
  source       = "./userdata"
  cluster_name = "${aws_ecs_cluster.api.name}"
}
