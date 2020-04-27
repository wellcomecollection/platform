resource "aws_launch_configuration" "launch_config" {
  security_groups = local.instance_security_groups

  key_name                    = var.key_name
  image_id                    = var.image_id
  instance_type               = var.instance_type
  iam_instance_profile        = aws_iam_instance_profile.instance_profile.name
  user_data                   = var.user_data
  associate_public_ip_address = var.associate_public_ip_address

  lifecycle {
    create_before_destroy = true
  }
}
