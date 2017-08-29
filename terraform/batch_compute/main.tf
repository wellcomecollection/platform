module "compute_environment_iam" {
  source = "batch_iam"
}

module "compute_environment_tpl" {
  source = "compute_environment"
  name   = "${var.name}"

  ec2_key_pair = "${var.key_name}"

  subnets = "${var.subnets}"

  ecs_instance_role = "${module.compute_environment_iam.ecs_instance_role_arn}"
  spot_fleet_role   = "${module.compute_environment_iam.spot_fleet_role_arn}"
  service_role      = "${module.compute_environment_iam.batch_service_role_arn}"

  admin_cidr_ingress = "${var.admin_cidr_ingress}"
  vpc_id             = "${var.vpc_id}"
  image_id           = "${var.image_id}"
}

resource "null_resource" "export_rendered_template" {
  depends_on = ["module.compute_environment_tpl", "module.compute_environment_iam"]

  triggers {
    template = "${module.compute_environment_tpl.rendered_template}"
  }

  provisioner "local-exec" {
    command = "cat > /app/batch_compute_environment_${var.name}.json <<EOL\n${module.compute_environment_tpl.rendered_template}\nEOL"

    on_failure = "fail"
  }

  provisioner "local-exec" {
    command = "aws_batch_helper compute create /app/batch_compute_environment_${var.name}.json"

    on_failure = "fail"
  }
}
