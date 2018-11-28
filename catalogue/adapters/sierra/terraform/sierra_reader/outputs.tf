output "demultiplexer_arn" {
  value = "${module.s3_demultiplexer_lambda.arn}"
}

output "topic_name" {
  value = "${module.demultiplexer_topic.name}"
}
