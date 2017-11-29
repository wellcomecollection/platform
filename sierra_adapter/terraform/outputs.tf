output "bibs_dynamo_stream_arn" {
  value = "${module.sierra_to_dynamo_bibs.dynamo_stream_arn}"
}

output "items_dynamo_stream_arn" {
  value = "${module.sierra_to_dynamo_items.dynamo_stream_arn}"
}
