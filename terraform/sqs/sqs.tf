resource "aws_sqs_queue" "q" {
  name           = "${var.queue_name}"
  policy         = "${data.aws_iam_policy_document.write_to_queue.json}"
  redrive_policy = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.dlq.arn}\",\"maxReceiveCount\":${var.max_receive_count}}"
}

resource "aws_sqs_queue" "dlq" {
  name = "${var.queue_name}_dlq"
}

resource "aws_cloudwatch_metric_alarm" "dlq_not_empty" {
  alarm_name          = "${aws_sqs_queue.dlq.name}_not_empty"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "NumberOfMessagesSent"
  namespace           = "SQS"
  period              = 60
  threshold           = 0
  statistic           = "SampleCount"

  dimensions {
    QueueName = "${aws_sqs_queue.dlq.name}"
  }
}
