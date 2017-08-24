output "name" {
  value = "${var.name}"
}

output "ec2_instance_profile" {
  value = "${module.compute_environment_iam.ec2_instance_profile}"
}