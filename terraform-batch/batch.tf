module "aws_batch_compute" {
  source = "./batch_compute"

  subnets            = "${join(",", formatlist("\"%s\"", module.vpc_batch.subnets))}"
  key_name           = "${var.key_name}"
  admin_cidr_ingress = "${var.admin_cidr_ingress}"
  vpc_id             = "${module.vpc_batch.vpc_id}"
  image_id = "ami-84c937fd"
}

module "aws_batch_queue" {
  source = "./batch_queue"

  name             = "default"
  compute_env_name = "${module.aws_batch_compute.name}"
}

module "aws_batch_job_tif-conversion" {
  source = "./batch_job"
  memory = "2048"
  name         = "tif-conversion"
  image_uri    = "${module.ecr_repository_tif-metadata.repository_url}:${var.release_ids["tif-metadata"]}"
  job_role_arn = "${module.batch_tif_conversion_iam.task_role_arn}"
  command      = ["/run.py", "--bucket-name", "Ref::bucket_name", "--key", "Ref::key"]
}
