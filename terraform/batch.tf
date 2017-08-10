module "aws_batch_compute" {
  source = "./batch_compute"

  subnets            = "${join(",", formatlist("\"%s\"", module.vpc_batch.subnets))}"
  key_name           = "${var.key_name}"
  admin_cidr_ingress = "${var.admin_cidr_ingress}"
  vpc_id             = "${module.vpc_batch.vpc_id}"
}

module "aws_batch_queue" {
  source = "./batch_queue"

  name             = "default"
  compute_env_name = "${module.aws_batch_compute.name}"
}

module "aws_batch_job_tif-conversion" {
  source = "./batch_job"

  name         = "tif-conversion"
  image_uri    = "${module.ecr_repository_tif-metadata.repository_url}:${var.release_ids["tif-metadata"]}"
  job_role_arn = "${module.batch_example_iam.task_role_arn}"
  command = ["/run.py", "--src-bucket=${var.tif-source-bucket}", "--src-key=Ref::", "--dst-key=<DST>", "--delete-original"]
}
