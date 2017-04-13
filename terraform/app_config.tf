data "template_file" "transformer" {
  template = "${file("transformer.conf.template")}"

  vars {
    stream_arn = "${aws_dynamodb_table.calm_table.stream_arn}"
    sns_arn    = "${aws_sns_topic.id_minter_topic.arn}"
  }
}

resource "aws_s3_bucket_object" "transformer_config" {
  bucket = "${var.infra_bucket}"
  acl = "private"
  key = "config/alex/transformer.conf"
  content = "${data.template_file.transformer.rendered}"
}

data "template_file" "calm_adapter" {
  template = "${file("calm_adapter.conf.template")}"

  vars {
    table_name = "${aws_dynamodb_table.calm_table.name}"
    sns_arn    = "${aws_sns_topic.service_scheduler_topic.arn}"
  }
}

resource "aws_s3_bucket_object" "calm_adapter_config" {
  bucket = "${var.infra_bucket}"
  acl = "private"
  key = "config/alex/calm_adapter.conf"
  content = "${data.template_file.calm_adapter.rendered}"
}
