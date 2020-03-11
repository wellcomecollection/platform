output "instance_security_groups" {
  value = local.instance_security_groups
}

output "ssh_controlled_ingress" {
  value = aws_security_group.ssh_controlled_ingress.*.id
}
