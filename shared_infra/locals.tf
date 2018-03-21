locals {
  ecr_pushes_topic_name    = "${data.terraform_remote_state.monitoring.ecr_pushes_topic_name}"
  lambda_pushes_topic_name = "${data.terraform_remote_state.monitoring.lambda_pushes_topic_name}"
}
