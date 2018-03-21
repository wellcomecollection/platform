output "ecr_pushes_topic_name" {
  value = "${module.ecr_pushes_topic.name}"
}

output "lambda_pushes_topic_name" {
  value = "${module.lambda_pushes_topic.name}"
}
