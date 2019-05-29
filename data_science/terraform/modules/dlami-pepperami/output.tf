output "instance_profile_name" {
  value = "${module.instance_profile.name}"
}

output "instance_profile_role_name" {
  value = "${module.instance_profile.role_name}"
}

output "ssh_controlled_ingress_sg" {
  value = "${module.security_groups.ssh_controlled_ingress}"
}

output "public_ip" {
  value = "${aws_instance.data.public_ip}"
}