# AWS Lambda

This module provides:

- A lambda function
- A DLQ by default
- Cloudwatch logging
- Triggers for events from:
  - Dynamo
  - SNS
  - Cloudwatch

## Usage

This module expects you to provide a properly packaged lambda in S3 at the given location.

```tf
module "my_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"

  name        = "lambda"
  description = "Schedules the reindexer based on the ReindexerTracker table"

  environment_variables = {
    MY_ENV_VAR     = "${var.my_env_var}"
  }

  s3_bucket       = "my_s3_bucket"
  s3_key          = "lambdas/my_lambda.zip"

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
}

module "my_lambda_dynamo_trigger" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_dynamo?ref=v1.0.0"

  stream_arn    = "${var.stream_arn}"

  function_arn  = "${module.my_lambda.arn}"
  function_role = "${module.my_lambda.role_name}"
}
```

## Outputs

- `arn`: ARN of the Lambda function
- `function_name`: Name of the Lambda function
- `role_name`: Name of the IAM role for this Lambda
