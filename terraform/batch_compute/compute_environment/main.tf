data "template_file" "compute_environment" {
  template = "${file("${path.module}/create_compute_environment.json.template")}"

  vars {
    name = "${var.name}"

    min_vcpus     = "${var.min_vcpus}"
    max_vcpus     = "${var.max_vcpus}"
    desired_vcpus = "${var.desired_vcpus}"

    subnets         = "${var.subnets}"
    security_groups = "${aws_security_group.instance_sg.id}"

    ec2_key_pair      = "${var.ec2_key_pair}"
    ecs_instance_role = "${var.ecs_instance_role}"

    bid_percentage  = "${var.bid_percentage}"
    spot_fleet_role = "${var.spot_fleet_role}"

    service_role = "${var.service_role}"
    ami_image_id = "${var.image_id}"
  }
}
