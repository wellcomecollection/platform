module "alb_500_errors" {
  source = "./alarms"
  name   = "${var.name}-alb-500-errors"

  metric    = "HTTPCode_ELB_5XX_Count"
  topic_arn = "${var.server_error_alarm_topic_arn}"
  dimension = "aws_alb.ecs_service.arn_suffix"
}

module "target_500_errors" {
  source = "./alarms"
  name   = "${var.name}-target-500-errors"

  metric    = "HTTPCode_Target_5XX_Count"
  topic_arn = "${var.server_error_alarm_topic_arn}"
  dimension = "aws_alb.ecs_service.arn_suffix"
}

module "alb_400_errors" {
  source = "./alarms"
  name   = "${var.name}-alb-400-errors"

  metric    = "HTTPCode_ELB_4XX_Count"
  topic_arn = "${var.client_error_alarm_topic_arn}"
  dimension = "aws_alb.ecs_service.arn_suffix"
}

module "target_400_errors" {
  source = "./alarms"
  name   = "${var.name}-target-400-errors"

  metric    = "HTTPCode_Target_4XX_Count"
  topic_arn = "${var.client_error_alarm_topic_arn}"
  dimension = "aws_alb.ecs_service.arn_suffix"
}
