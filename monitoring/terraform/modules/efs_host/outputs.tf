output "ssh_controlled_ingress_sg" {
  value = "${module.asg.ssh_controlled_ingress_sg}"
}

output "efs_host_path" {
  value = "${var.efs_host_path}"
}
