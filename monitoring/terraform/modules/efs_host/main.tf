module "asg" {
  source = "../ondemand"

  name = var.asg_name

  image_id = var.image_id

  custom_security_groups = var.custom_security_groups

  subnet_list = var.subnets
  vpc_id      = var.vpc_id
  key_name    = var.key_name
  user_data   = data.template_file.userdata.rendered

  instance_type = var.instance_type
}

data "template_file" "userdata" {
  template = file("${path.module}/efs.tpl")

  vars = {
    cluster_name  = var.cluster_name
    efs_fs_id     = var.efs_fs_id
    efs_host_path = var.efs_host_path
    region        = var.region
  }
}
