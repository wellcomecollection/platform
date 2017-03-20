resource "aws_sns_topic" "ingest_topic" {
  name = "es_ingest"
}

resource "aws_sns_topic" "service_scheduler_topic" {
  name = "service_scheduler"
}
