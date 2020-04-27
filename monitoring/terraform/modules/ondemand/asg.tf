resource "aws_cloudformation_stack" "ecs_asg" {
  name          = var.name
  template_body = data.template_file.cluster_ecs_asg.rendered

  lifecycle {
    create_before_destroy = true
  }
}

data "template_file" "cluster_ecs_asg" {
  template = file("${path.module}/asg.json.template")

  vars = {
    launch_config_name  = aws_launch_configuration.launch_config.name
    vpc_zone_identifier = jsonencode(var.subnet_list)
    asg_min_size        = 1
    asg_desired_size    = 1
    asg_max_size        = 2
    asg_name            = var.name
  }
}
