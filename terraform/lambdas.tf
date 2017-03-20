resource "aws_iam_role" "awlc_iam_role" {
  name = "awlc_iam_role"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_iam_role_policy" "awlc_iam_role_policy" {
  name = "update_ecs_service_size"
  role = "${aws_iam_role.awlc_iam_role.name}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecs:UpdateService"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": "logs:CreateLogGroup",
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": ["${aws_cloudwatch_log_group.update_ecs_service_size.arn}"]
    }
  ]
}
EOF
}


resource "aws_lambda_function" "update_ecs_service_size" {
  filename      = "../lambdas/update_ecs_service_size.py.zip"
  description   = "Update the desired count of a service running in ECS"
  function_name = "update_ecs_service_size"
  role          = "${aws_iam_role.awlc_iam_role.arn}"
  handler       = "update_ecs_service_size.main"
  runtime       = "python2.7"
  depends_on = [
    "aws_iam_role_policy.awlc_iam_role_policy"
  ]
}

resource "aws_sns_topic_subscription" "topic_lambda" {
  topic_arn = "${aws_sns_topic.service_scheduler_topic.arn}"
  protocol  = "lambda"
  endpoint  = "${aws_lambda_function.update_ecs_service_size.arn}"
}

resource "aws_lambda_permission" "with_sns" {
    statement_id = "AllowExecutionFromSNS"
    action = "lambda:InvokeFunction"
    function_name = "${aws_lambda_function.update_ecs_service_size.arn}"
    principal = "sns.amazonaws.com"
    source_arn = "${aws_sns_topic.service_scheduler_topic.arn}"
}





resource "aws_cloudwatch_event_rule" "once_a_day" {
  name = "once-a-day"
  description = "Fires once a day"
  schedule_expression = "rate(1 minute)"
}

resource "aws_cloudwatch_event_target" "check_foo_every_five_minutes" {
    rule = "${aws_cloudwatch_event_rule.once_a_day.name}"
    arn = "${aws_lambda_function.publish_to_sns.arn}"
    input = "{\"hello\": \"world\"}"
}

resource "aws_lambda_permission" "allow_cloudwatch_to_call_check_foo" {
    statement_id = "AllowExecutionFromCloudWatch"
    action = "lambda:InvokeFunction"
    function_name = "${aws_lambda_function.publish_to_sns.function_name}"
    principal = "events.amazonaws.com"
    source_arn = "${aws_cloudwatch_event_rule.once_a_day.arn}"
}









resource "aws_iam_role" "awlc_iam_role_trigger" {
  name = "awlc_iam_role_trigger"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_iam_role_policy" "awlc_iam_role_policy_trigger" {
  name = "publish_to_sns_policy"
  role = "${aws_iam_role.awlc_iam_role_trigger.name}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sns:Publish"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": ["${aws_cloudwatch_log_group.publish_to_sns.arn}"]
    }
  ]
}
EOF
}


resource "aws_lambda_function" "publish_to_sns" {
  filename      = "../lambdas/publish_to_sns.py.zip"
  description   = "Publish to SNS"
  function_name = "publish_to_sns"
  role          = "${aws_iam_role.awlc_iam_role_trigger.arn}"
  handler       = "publish_to_sns.main"
  runtime       = "python2.7"
  depends_on = [
    "aws_iam_role_policy.awlc_iam_role_policy_trigger"
  ]
}

resource "aws_cloudwatch_log_group" "publish_to_sns" {
  name = "/aws/lambda/publish_to_sns"
}

resource "aws_cloudwatch_log_group" "update_ecs_service_size" {
  name = "/aws/lambda/update_ecs_service_size"
}










