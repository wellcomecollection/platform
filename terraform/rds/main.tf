resource "aws_rds_cluster_instance" "cluster_instances" {
  count = 2

  identifier           = "${var.cluster_identifier}-${count.index}"
  cluster_identifier   = "${aws_rds_cluster.default.id}"
  instance_class       = "db.t2.small"
  db_subnet_group_name = "${aws_db_subnet_group.default.name}"
  publicly_accessible  = true
}

resource "aws_db_subnet_group" "default" {
  subnet_ids = ["${var.vpc_subnet_ids}"]
}

resource "aws_rds_cluster" "default" {
  db_subnet_group_name   = "${aws_db_subnet_group.default.name}"
  cluster_identifier     = "${var.cluster_identifier}"
  database_name          = "${var.database_name}"
  master_username        = "${var.username}"
  master_password        = "${var.password}"
  vpc_security_group_ids = ["${aws_security_group.database_sg.id}"]
}
