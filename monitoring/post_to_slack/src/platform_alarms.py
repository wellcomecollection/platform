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


def should_be_sent_to_main_channel(alarm_name):
    """Should this alarm be sent to the main channel?"""
    # Alarms for the API or Loris always go the main channel.
    if any(p in alarm_name for p in ['api_remus', 'api_romulus', 'loris']):
        return True

    # Otherwise default to False, because we don't know what this alarm is.
    return False


def is_critical_error(alarm_name):
    """Is this a critical error (True) or just a warning (False)?"""
    # Alarms for the API or Loris are always critical.
    if any(p in alarm_name for p in ['api_remus', 'api_romulus', 'loris']):
        return True

    # Any alarms to do with healthy/unhealthy hosts are critical.
    if alarm_name.endswith((
        '-not-enough-healthy-hosts',
        '-unhealthy-hosts',
        '-500-errors',
        '_TerminalFailure',
    )):
        return True

    # DLQ errors are warnings, not errors.
    if alarm_name.endswith(('_dlq_not_empty',)):
        return False

    # Lambda errors are warnings, not errors.
    if alarm_name.startswith('lambda-') and alarm_name.endswith('-errors'):
        return False

    # Otherwise default to True, because we don't know what this alarm is.
    return True
