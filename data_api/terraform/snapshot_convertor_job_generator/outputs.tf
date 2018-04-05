output "topic_name" {
  value = "${module.snapshot_convertor_topic.name}"
}

output "function_arn" {
  value = "${module.snapshot_convertor_job_generator_lambda.arn}"
}
