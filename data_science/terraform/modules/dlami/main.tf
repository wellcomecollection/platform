module "security_groups" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/modules/security_groups?ref=v11.7.2"

  name   = "${var.name}"
  vpc_id = "${var.vpc_id}"

  custom_security_groups         = ["${var.custom_security_groups}"]
  controlled_access_cidr_ingress = ["${var.controlled_access_cidr_ingress}"]
}

resource "aws_ebs_volume" "volume" {
  availability_zone = "${data.aws_subnet.selected.availability_zone}"
  size              = "${var.ebs_volume_size}"

  tags = {
    Name = "${var.name}"
  }
}

resource "aws_volume_attachment" "ebs_attachment" {
  device_name = "/dev/sdh"
  volume_id   = "${aws_ebs_volume.volume.id}"
  instance_id = "${aws_instance.data.id}"
}

module "instance_profile" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/modules/instance_profile?ref=v11.7.2"

  name = "${var.name}"
}

resource "aws_instance" "data" {
  ami           = "${data.aws_ami.ubuntu.id}"
  instance_type = "${var.instance_type}"

  associate_public_ip_address = true

  user_data = "${data.template_file.userdata.rendered}"

  ebs_optimized = true

  subnet_id = "${data.aws_subnet.selected.id}"

  vpc_security_group_ids = ["${module.security_groups.instance_security_groups}"]

  iam_instance_profile = "${module.instance_profile.name}"

  key_name ="${var.key_name}"

  tags = {
    Name = "${var.name}"
  }
}