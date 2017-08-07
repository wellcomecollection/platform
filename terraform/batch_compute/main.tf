module "compute_environment_iam" {
  source = "./batch_iam"
}

module "compute_environment_tpl" {
  source = "./compute_environment"

  ec2_key_pair = "${var.key_name}"

  subnets = "${var.subnets}"

  ecs_instance_role = "${module.compute_environment_iam.ecs_instance_role_arn}"
  spot_fleet_role = "${module.compute_environment_iam.spot_fleet_role_arn}"
  service_role = "${module.compute_environment_iam.batch_service_role_arn}"

  admin_cidr_ingress = "${var.admin_cidr_ingress}"
  vpc_id = "${var.vpc_id}"
}

resource "null_resource" "export_rendered_template" {
  provisioner "local-exec" {
    command = "cat > test_output.json <<EOL\n${module.compute_environment_tpl.rendered_template}\nEOL"
  }
}