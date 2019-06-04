data "template_file" "requirements" {
  template = "${file("${path.module}/templates/requirements.txt")}"
}

data "template_file" "jupyter_config" {
  template = "${file("${path.module}/templates/jupyter_notebook_config.py.tpl")}"

  vars {
    notebook_port   = "8888"
    hashed_password = "${var.hashed_password}"
  }
}

data "template_file" "userdata" {
  template = "${file("${path.module}/templates/userdata.sh.tpl")}"

  vars {
    jupyter_notebook_config = "${data.template_file.jupyter_config.rendered}"
    requirements            = "${data.template_file.requirements.rendered}"
    anaconda_path           = "/home/ec2-user/anaconda3"
    default_environment     = "${var.default_environment}"
    efs_mount_id            = "${var.efs_mount_id}"
  }
}

data "aws_ami" "ubuntu" {
  owners = ["amazon"]

  filter {
    name = "name"
    values = ["Deep Learning AMI (Amazon Linux 2)*"]
  }

  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  most_recent = true
}

data "aws_subnet_ids" "public" {
  vpc_id = "${var.vpc_id}"

  tags {
    Availability = "public"
  }
}

data "aws_subnet" "selected" {
  id = "${data.aws_subnet_ids.public.ids[0]}"
}