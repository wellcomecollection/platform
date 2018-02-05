# -*- encoding: utf-8
"""
Platform-specific logic for our Slack alarms.
"""


def guess_cloudwatch_log_group(alarm):
    """
    Guess the name of the CloudWatch log group most likely to contain
    logs about the error.
    """
    if alarm.name.startswith('loris-'):
        return 'platform/loris'

    if alarm.name.startswith('api_romulus_v1-'):
        return 'platform/api_romulus_v1'

    if alarm.name.startswith('api_remus_v1-'):
        return 'platform/api_remus_v1'

    if alarm.name.startswith('lambda-'):
        # e.g. lambda-ecs_ec2_instance_tagger-errors
        lambda_name = alarm.name.split('-')[1]
        return f'/aws/lambda/{lambda_name}'

    raise ValueError(
        f"Unable to guess log group name for alarm name={alarm.name!r}"
    )


def guess_cloudwatch_search_terms(alarm):
    """Guess some search terms that might be useful in CloudWatch."""
    if alarm.name == 'loris-alb-target-500-errors':
        return ['"HTTP/1.0 500"']

    if alarm.name.startswith('lambda'):
        return ['Traceback', 'Task timed out after']

    if alarm.name.startswith('api_') and alarm.name.endswith('-500-errors'):
        return ['"HTTP 500"']

    return []
