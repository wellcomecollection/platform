resource "aws_cloudwatch_metric_alarm" "ec2_instance_terminating_for_too_long" {
  alarm_name          = "${aws_cloudformation_stack.ecs_asg.outputs["AsgName"]}_instance_terminating_for_too_long"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 60
  metric_name         = "GroupTerminatingInstances"
  namespace           = "AWS/AutoScaling"
  period              = 60
  threshold           = 0
  statistic           = "Average"

  dimensions {
    AutoScalingGroupName = "${aws_cloudformation_stack.ecs_asg.outputs["AsgName"]}"
  }

  alarm_actions = ["${var.alarm_topic_arn}"]
}
