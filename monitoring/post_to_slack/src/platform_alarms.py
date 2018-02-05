# -*- encoding: utf-8
"""
Platform-specific logic for our Slack alarms.
"""

import re


def guess_cloudwatch_log_group(alarm_name):
    """
    Guess the name of the CloudWatch log group most likely to contain
    logs about the error.
    """
    if alarm_name.startswith('loris-'):
        return 'platform/loris'

    if alarm_name.startswith('api_romulus_v1-'):
        return 'platform/api_romulus_v1'

    if alarm_name.startswith('api_remus_v1-'):
        return 'platform/api_remus_v1'

    if alarm_name.startswith('lambda-'):
        # e.g. lambda-ecs_ec2_instance_tagger-errors
        lambda_name = alarm_name.split('-')[1]
        return f'/aws/lambda/{lambda_name}'

    raise ValueError(
        f"Unable to guess log group name for alarm name={alarm_name!r}"
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


def simplify_message(message):
    """
    Sometimes a CloudWatch message includes information that we don't want
    to appear in Slack -- e.g. date/time.

    This function tries to strip out extra bits from the message, so we get
    a tight and focused error appearing in Slack.
    """
    # Scala messages have a prefix that gives us a timestamp and thread info:
    #
    #     14:02:47.249 [ForkJoinPool-2-worker-17]
    #
    # Bin it!
    message = re.sub(
        r'\d{2}:\d{2}:\d{2}\.\d{3} \[ForkJoinPool-\d+-worker-\d+\] ', '',
        message
    )

    # Loris messages have a bunch of origin and UWSGI information as a prefix:
    #
    #     [pid: 86|app: 0|req: 195/3682] 172.17.0.5 () {40 vars in 879
    #       bytes} [Tue Oct 10 19:37:06 2017]
    #
    # Discard it!
    message = re.sub(
        r'\[pid: \d+\|app: \d+\|req: \d+/\d+\] '
        r'\d+\.\d+\.\d+\.\d+ \(\) '
        r'{\d+ vars in \d+ bytes} '
        r'\[[A-Za-z0-9: ]+\]', '', message
    )

    # Loris messages also have an uninteresting suffix:
    #
    #     3 headers in 147 bytes (1 switches on core 0)
    #
    # Throw it away!
    message = re.sub(
        r'\d+ headers in \d+ bytes \(\d+ switches on core \d+\)', '', message
    )

    # Loris logs tell us information that isn't helpful for debugging:
    #
    #      => generated 271 bytes in 988 msecs
    #
    # Expunge-inate!
    message = re.sub(r'=> generated \d+ bytes in \d+ msecs ', '', message)

    # Lambda timeouts have an opaque prefix:
    #
    #     2017-10-12T13:18:31.917Z d1fdfca5-af4f-11e7-a100-030f2a39c6f6 Task
    #     timed out after 10.01 seconds
    #
    # Drop it!
    message = re.sub(
        r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z '
        r'[0-9a-f-]+ (?=Task timed out)', '', message
    )

    return message.strip()
