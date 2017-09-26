output "instance_sg_id" {
  value = "${module.launch_config.instance_sg_id}"
}

output "loadbalancer_sg_https_id" {
  value = "${module.launch_config.loadbalancer_sg_https_id}"
}

output "loadbalancer_sg_http_id" {
  value = "${module.launch_config.loadbalancer_sg_http_id}"
}

output "asg_name" {
  value = "${module.cloudformation_stack.asg_name}"
}

output "asg_desired" {
  value = "${var.asg_desired}"
}

output "asg_max" {
  value = "${var.asg_max}"
}
