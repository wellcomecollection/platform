module "loris_userdata" {
  source = "../../terraform/userdata"
  cluster_name = "${aws_ecs_cluster.loris.name}"
  efs_filesystem_id = "${module.loris_efs.efs_id}"
}
module "loris_userdata_ebs" {
  source            = "../../terraform/userdata"
  cluster_name      = "${aws_ecs_cluster.loris_ebs.name}"
  efs_filesystem_id = "${module.loris_efs.efs_id}"
}
