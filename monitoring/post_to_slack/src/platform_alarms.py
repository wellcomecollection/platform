# -*- encoding: utf-8
"""
Platform-specific logic for our Slack alarms.
"""

import re

from cloudwatch_alarms import ThresholdMessage


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


def guess_cloudwatch_search_terms(alarm_name):
    """Guess some search terms that might be useful in CloudWatch."""
    if alarm_name == 'loris-alb-target-500-errors':
        return ['"HTTP/1.0 500"']

    if alarm_name.startswith('lambda'):
        return ['Traceback', 'Task timed out after']

    if alarm_name.startswith('api_') and alarm_name.endswith('-500-errors'):
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


def get_human_message(alarm_name, state_reason):
    """
    Sometimes we can provide a more human-readable message than
    "Threshold Crossed".  Try to do so, if possible.
    """
    threshold = ThresholdMessage.from_message(state_reason)

    # For a DLQ, the lower threshold is always going to be zero, so it's
    # enough to state how many items were on the DLQ.  For example:
    #
    #   There is 1 item on the ID minter DLQ.
    #
    if alarm_name.endswith('_dlq_not_empty'):
        queue_name = alarm_name[:-len('_dlq_not_empty')]
        queue_length = threshold.actual_value

        if queue_length == 1:
            message = 'There is 1 item'
        else:
            message = f'There are {queue_length} items'

        return message + f' on the {queue_name} DLQ.'

    # For unhealthy hosts, the lower threshold is always going to be zero.
    # For example:
    #
    #   There are 2 unhealthy targets in the id_minter ALB target group.
    #
    if alarm_name.endswith('-alb-unhealthy-hosts'):
        group_name = alarm_name[:-len('-alb-unhealthy-hosts')]
        unhealthy_host_count = threshold.actual_value

        if unhealthy_host_count == 1:
            message = 'There is 1 unhealthy target'
        else:
            message = f'There are {unhealthy_host_count} unhealthy targets'

        return message + f' in the {group_name} ALB target group.'

    # For not-enough-healthy hosts, the lower threshold may be different,
    # so we include that in the message.  For example:
    #
    #   There aren't enough healthy targets in the ingestor
    #   (saw 2, expected at least 3).
    #
    if alarm_name.endswith('-alb-not-enough-healthy-hosts'):
        group_name = alarm_name[:-len('-alb-not-enough-healthy-hosts')]

        if threshold.is_breaching:
            return f'There are no healthy hosts in the {group_name} ALB target group.'
        else:
            desired_count = threshold.desired_value
            actual_count = threshold.actual_value

            return (
                f"There aren't enough healthy targets in the {group_name} ALB target group "
                f"(saw {actual_count}, expected at least {desired_count})."
            )

    # Any number of 500 errors is bad!  For example:
    #
    #   There were multiple 500 errors (3) from the ingestor ALB target group.
    #
    # We put the numeral in brackets just to make the sentence easier to read.
    if alarm_name.endswith('-alb-target-500-errors'):
        group_name = alarm_name[:-len('-alb-target-500-errors')]
        error_count = threshold.actual_value

        if error_count == 1:
            return f'There was a 500 error from the {group_name} ALB target group.'
        else:
            return f'There were multiple 500 errors ({error_count}) from the {group_name} ALB target group.'

    # As are any number of Lambda errors.  Example:
    #
    #   There was an error in the post_to_slack Lambda.
    #
    if alarm_name.startswith('lambda-') and alarm_name.endswith('-errors'):
        lambda_name = alarm_name[len('lambda-'):-len('-errors')]
        error_count = threshold.actual_value

        if error_count == 1:
            return f'There was an error in the {lambda_name} Lambda.'
        else:
            return f'There were {error_count} errors in the {lambda_name} Lambda.'

    return state_reason
