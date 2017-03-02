/*
  Once records have been retrieved from DynamoDB and transformed into
  a JSON form suitable for Elasticsearch ingestion, they're sent to
  an SNS topic, which in turn pushes them to an SQS queue.  We have a worker
  that reads records from this queue and actually pushes them to Elasticsearch.

  This file sets up the SNS topic, SQS queue, and necessary policy/subscription
  for them to work together.
*/


/*
  We'll use this to get the account ID later.
  https://www.terraform.io/docs/providers/aws/d/caller_identity.html
*/
data "aws_caller_identity" "current" { }


/*
  Names of the SNS topic and SQS queue, which appear both in the resource
  definitions and the policy.
*/
variable "es_ingest" {
  default {
    sqs_name = "es-ingest-queue"
    sns_name = "es-ingest-topic"
  }
}


/*
  This defines a policy that allows SNS to push messages to the SQS queue.
  Because the policy relies on the SNS/SQS ARNs, which don't exist yet (and
  which we can't reference without creating a circular dependency), we infer
  their ARNs instead.

  Based on this example: https://github.com/hashicorp/terraform/issues/6909
*/
data "aws_iam_policy_document" "sqs_queue_policy" {
  statement {
    sid     = "es-sns-to-sqs-policy"
    effect  = "Allow"

    principals {
      type        = "AWS"
      identifiers = ["*"]
    }

    actions = [
      "sqs:SendMessage"
    ]

    resources = [
      "arn:aws:sqs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:${var.es_ingest["sqs_name"]}"
    ]

    condition {
      test      = "ArnEquals"
      variable  = "aws:SourceArn"

      values = [
        "arn:aws:sns:${var.aws_region}:${data.aws_caller_identity.current.account_id}:${var.es_ingest["sns_name"]}"
      ]
    }
  }
}


/*
  Define the policy.
*/
resource "aws_sns_topic" "es_ingest_topic" {
  name = "${var.es_ingest["sns_name"]}"
}


/*
  Define the queue.  As a future enhancement, it would be nice to make this
  a FIFO queue, but that's currently not available in eu-west-1.
  http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues.html
*/
resource "aws_sqs_queue" "es_ingest_queue" {
  name    = "${var.es_ingest["sqs_name"]}"
  policy  = "${data.aws_iam_policy_document.sqs_queue_policy.json}"
}


/*
  Finally, we have to create a subscription for the SNS topic to be able
  to push messages to the SQS queue.
*/
resource "aws_sns_topic_subscription" "sns_topic" {
  topic_arn = "${aws_sns_topic.es_ingest_topic.arn}"
  protocol  = "sqs"
  endpoint  = "${aws_sqs_queue.es_ingest_queue.arn}"
}
