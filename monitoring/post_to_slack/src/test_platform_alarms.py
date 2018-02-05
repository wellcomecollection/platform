# -*- encoding: utf-8

import attr
import pytest

from platform_alarms import (
    get_human_message,
    guess_cloudwatch_log_group,
    guess_cloudwatch_search_terms,
    is_critical_error,
    should_be_sent_to_main_channel,
    simplify_message
)


@attr.s
class Alarm:
    name = attr.ib()


@pytest.mark.parametrize('alarm_name, expected_log_group_name', [
    ('loris-alb-target-500-errors', 'platform/loris'),
    ('loris-alb-not-enough-healthy-hosts', 'platform/loris'),
    ('api_romulus_v1-alb-target-400-errors', 'platform/api_romulus_v1'),
    ('api_remus_v1-alb-target-500-errors', 'platform/api_remus_v1'),
    ('lambda-ecs_ec2_instance_tagger-errors', '/aws/lambda/ecs_ec2_instance_tagger'),
    ('lambda-post_to_slack-errors', '/aws/lambda/post_to_slack'),
])
def test_guess_cloudwatch_log_group(alarm_name, expected_log_group_name):
    assert guess_cloudwatch_log_group(alarm_name) == expected_log_group_name


@pytest.mark.parametrize('bad_alarm_name', [
    'api_remus_v2-alb-target-500-errors',
    'winnipeg-not-enoguh-healthy-hosts',
])
def test_unrecognised_log_group_name_is_valueerror(bad_alarm_name):
    with pytest.raises(ValueError):
        guess_cloudwatch_log_group(bad_alarm_name)


@pytest.mark.parametrize('alarm_name, expected_search_terms', [
    ('loris-alb-target-500-errors', ['"HTTP/1.0 500"']),
    ('loris-alb-not-enough-healthy-hosts', []),
    ('api_romulus_v1-alb-target-400-errors', []),
    ('api_remus_v1-alb-target-500-errors', ['"HTTP 500"']),
    ('lambda-ecs_ec2_instance_tagger-errors', ['Traceback', 'Task timed out after']),
    ('lambda-post_to_slack-errors', ['Traceback', 'Task timed out after']),
])
def test_guess_cloudwatch_search_terms(alarm_name, expected_search_terms):
    assert guess_cloudwatch_search_terms(alarm_name) == expected_search_terms


@pytest.mark.parametrize('alarm_name, should_send_to_main', [
    ('api_romulus_v1-alb-target-400-errors', True),
    ('api_remus_v1-alb-target-500-errors', True),
    ('loris-alb-not-enough-healthy-hosts', True),
    ('id_minter-alb-unhealthy-hosts', False),
    ('ingestor-alb-unhealthy-hosts', False),
    ('transformer-alb-not-enough-healthy-hosts', False),
    ('grafana-alb-target-500-errors', False),
    ('IngestorWorkerService_TerminalFailure', False),
    ('sierra_items_windows_dlq_not_empty', False),
    ('lambda-post_to_slack-errors', False),
    ('unknown-alarm-type', False),
])
def test_should_be_sent_to_main_channel(alarm_name, should_send_to_main):
    assert should_be_sent_to_main_channel(alarm_name) == should_send_to_main


@pytest.mark.parametrize('alarm_name, expected_is_critical_error', [
    ('api_romulus_v1-alb-target-400-errors', True),
    ('api_remus_v1-alb-target-500-errors', True),
    ('loris-alb-not-enough-healthy-hosts', True),
    ('id_minter-alb-unhealthy-hosts', True),
    ('ingestor-alb-unhealthy-hosts', True),
    ('transformer-alb-not-enough-healthy-hosts', True),
    ('grafana-alb-target-500-errors', True),
    ('IngestorWorkerService_TerminalFailure', True),
    ('sierra_items_windows_dlq_not_empty', False),
    ('lambda-post_to_slack-errors', False),
    ('unknown-alarm-type', True),
])
def test_is_critical_error(alarm_name, expected_is_critical_error):
    assert is_critical_error(alarm_name) == expected_is_critical_error


@pytest.mark.parametrize('message, expected', [
    # We correctly strip timestamp and thread information from Scala logs
    (
        '13:25:56.965 [ForkJoinPool-1-worker-61] ERROR u.a.w.p.a.f.e.ElasticsearchResponseExceptionMapper - Sending HTTP 500 from ElasticsearchResponseExceptionMapper (Error (com.fasterxml.jackson.core.JsonParseException: Unrecognized token ‘No’: was expecting ‘null’, ‘true’, ‘false’ or NaN',
        'ERROR u.a.w.p.a.f.e.ElasticsearchResponseExceptionMapper - Sending HTTP 500 from ElasticsearchResponseExceptionMapper (Error (com.fasterxml.jackson.core.JsonParseException: Unrecognized token ‘No’: was expecting ‘null’, ‘true’, ‘false’ or NaN'
    ),

    # We strip UWGSI and timestamp prefixes from Loris logs
    (
        '[pid: 88|app: 0|req: 1871/9531] 172.17.0.4 () {46 vars in 937 bytes} [Wed Oct 11 22:42:03 2017] GET //wordpress:2014/05/untitled3.png/full/320,/0/default.jpg (HTTP/1.0 500)',
        'GET //wordpress:2014/05/untitled3.png/full/320,/0/default.jpg (HTTP/1.0 500)',
    ),

    # We strip UWSGI suffixes from Loris logs
    (
        'GET //wordpress:2014/05/untitled2.png/full/320,/0/default.jpg (HTTP/1.0 500) 3 headers in 147 bytes (1 switches on core 0)',
        'GET //wordpress:2014/05/untitled2.png/full/320,/0/default.jpg (HTTP/1.0 500)'
    ),

    # We strip byte count and timings from Loris logs
    (
        'GET //s3:L0009000/L0009709.jpg/full/282,/0/default.jpg => generated 271 bytes in 988 msecs (HTTP/1.0 500)',
        'GET //s3:L0009000/L0009709.jpg/full/282,/0/default.jpg (HTTP/1.0 500)',
    ),

    # We strip the timestamp and Lambda ID from timeout errors
    (
        '2017-10-12T13:18:31.917Z d1fdfca5-af4f-11e7-a100-030f2a39c6f6 Task timed out after 10.01 seconds',
        'Task timed out after 10.01 seconds'
    ),
])
def test_simplify_message(message, expected):
    assert simplify_message(message) == expected


@pytest.mark.parametrize('alarm_name, state_reason, expected_message', [
    (
        'sierra_items_windows_dlq_not_empty',
        'Threshold Crossed: 1 datapoint [1.0 (01/01/01 12:00:00)] was greater than the threshold (0.0).',
        'There is 1 item on the sierra_items_windows DLQ.'
    ),
    (
        'transformer_dlq_not_empty',
        'Threshold Crossed: 1 datapoint [3.0 (01/01/01 12:00:00)] was greater than the threshold (0.0).',
        'There are 3 items on the transformer DLQ.'
    ),
    (
        'id_minter_dlq_not_empty',
        'Threshold Crossed: 1 datapoint [17.0 (01/01/01 12:00:36)] was greater than the threshold (0.0).',
        'There are 17 items on the id_minter DLQ.'
    ),

    (
        'id_minter-alb-unhealthy-hosts',
        'Threshold Crossed: 1 datapoint [1.0 (01/01/01 12:00:00)] was greater than the threshold (0.0).',
        'There is 1 unhealthy target in the id_minter ALB target group.'
    ),
    (
        'loris-alb-unhealthy-hosts',
        'Threshold Crossed: 1 datapoint [3.0 (01/01/01 12:00:00)] was greater than the threshold (0.0).',
        'There are 3 unhealthy targets in the loris ALB target group.'
    ),

    (
        'api_romulus_v1-alb-not-enough-healthy-hosts',
        'Threshold Crossed: 1 datapoint [0.0 (09/01/18 10:36:00)] was less than the threshold (1.0).',
        "There aren't enough healthy targets in the api_romulus_v1 ALB target group (saw 0, expected at least 1)."
    ),
    (
        'ingestor-alb-not-enough-healthy-hosts',
        'Threshold Crossed: 1 datapoint [2.0 (09/01/18 10:36:00)] was less than the threshold (3.0).',
        "There aren't enough healthy targets in the ingestor ALB target group (saw 2, expected at least 3)."
    ),
    (
        'api_remus_v1-alb-not-enough-healthy-hosts',
        'Threshold Crossed: no datapoints were received for 1 period and 1 missing datapoint was treated as [Breaching].',
        'There are no healthy hosts in the api_remus_v1 ALB target group.'
    ),

    (
        'api_remus_v1-alb-target-500-errors',
        'Threshold Crossed: 1 datapoint [3.0 (11/08/18 10:55:00)] was greater than or equal to the threshold (1.0).',
        'There were multiple 500 errors (3) from the api_remus_v1 ALB target group.'
    ),
    (
        'grafana-alb-target-500-errors',
        'Threshold Crossed: 1 datapoint [1.0 (11/08/18 10:55:00)] was greater than or equal to the threshold (1.0).',
        'There was a 500 error from the grafana ALB target group.'
    ),

    (
        'lambda-update_ecs_service_size-errors',
        'Threshold Crossed: 1 datapoint [1.0 (02/02/18 13:20:00)] was greater than or equal to the threshold (1.0).)',
        'There was an error in the update_ecs_service_size Lambda.'
    ),
    (
        'lambda-post_to_slack-errors',
        'Threshold Crossed: 1 datapoint [4.0 (02/02/18 13:20:00)] was greater than or equal to the threshold (1.0).)',
        'There were 4 errors in the post_to_slack Lambda.'
    ),

    (
        'unrecognised-alarm-name',
        'Threshold Crossed: 1 datapoint [1.0 (01/01/01 12:00:00)] was less than the threshold (0.0).',
        'Threshold Crossed: 1 datapoint [1.0 (01/01/01 12:00:00)] was less than the threshold (0.0).',
    ),
])
def test_get_human_message(alarm_name, state_reason, expected_message):
    message = get_human_message(
        alarm_name=alarm_name, state_reason=state_reason
    )
    assert message == expected_message
