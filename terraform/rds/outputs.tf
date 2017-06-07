output "username" {
  value = "${var.username}"
}

output "password" {
  value = "${var.password}"
}

output "host" {
  value = "${aws_rds_cluster.default.endpoint}"
}

output "port" {
  value = "${aws_rds_cluster.default.port}"
}

output "database_name" {
  value = "${var.database_name}"
}
