module "aws_batch_compute" {
  source = "./batch_compute"

  name               = "example"
  subnets            = "${join(",", formatlist("\"%s\"", module.vpc_batch.subnets))}"
  key_name           = "${var.key_name}"
  admin_cidr_ingress = "${var.admin_cidr_ingress}"
  vpc_id             = "${module.vpc_batch.vpc_id}"
}

module "aws_batch_queue" {
  source = "./batch_queue"

  name             = "example"
  compute_env_name = "example"
}

module "aws_batch_job" {
  source = "./batch_job"

  name         = "example"
  image_uri    = "hello-world"
  job_role_arn = "${module.batch_example_iam.task_role_arn}"
}
