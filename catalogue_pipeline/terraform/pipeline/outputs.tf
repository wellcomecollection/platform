output "rds_access_security_group_id" {
  value = "${aws_security_group.rds_access_security_group.id}"
}

output "cluster_name" {
  value = "${var.namespace}"
}
