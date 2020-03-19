resource "aws_efs_file_system" "efs" {
  creation_token   = "grafana_efs"
  performance_mode = "generalPurpose"
}

resource "aws_efs_mount_target" "mount_target" {
  count           = length(local.private_subnets)
  file_system_id  = aws_efs_file_system.efs.id
  subnet_id       = local.private_subnets[count.index]
  security_groups = [aws_security_group.efs_mnt.id]
}

resource "aws_security_group" "efs_mnt" {
  description = "security groupt for efs mounts"
  vpc_id      = local.vpc_id
  name        = "grafana_efs_sg"

  ingress {
    protocol  = "tcp"
    from_port = 2049
    to_port   = 2049

    security_groups = [
      aws_security_group.efs_security_group.id,
    ]
  }
}

resource "aws_security_group" "efs_security_group" {
  name        = "${local.namespace}_efs_security_group"
  description = "Allow traffic between services and efs"
  vpc_id      = local.vpc_id

  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.namespace}-efs"
  }
}
