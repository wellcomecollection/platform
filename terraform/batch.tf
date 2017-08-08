module "aws_batch_compute" {
  source = "./batch_compute"

  name               = "adhoc"
  subnets            = "${join(",", formatlist("\"%s\"", module.vpc_batch.subnets))}"
  key_name           = "${var.key_name}"
  admin_cidr_ingress = "${var.admin_cidr_ingress}"
  vpc_id             = "${module.vpc_batch.vpc_id}"
}

module "aws_batch_queue" {
  source = "./batch_queue"

  name             = "adhoc"
  compute_env_name = "adhoc"
}

module "aws_batch_job" {
  source = "./batch_job"

  image_uri = "hello-world"
  jobRoleArn = "${module.batch_example_iam.task_role_arn}"
}
