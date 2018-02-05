import json
import os

import mock
import pytest

import post_to_slack


@pytest.fixture
def critical_hook():
    return 'https://api.slack.com/hooks/example_critical'


@pytest.fixture
def alarm_name():
    return 'api-alb-target-500-errors'


@pytest.fixture
def alarm_reason():
    return (
        "Threshold Crossed: 1 datapoint [1.0 (05/02/18 06:28:00)] was "
        "greater than or equal to the threshold (1.0)."
    )


@pytest.fixture
def event(critical_hook, alarm_name, alarm_reason):
    noncritical_hook = 'https://api.slack.com/hooks/example_non-critical'

    os.environ['CRITICAL_SLACK_WEBHOOK'] = critical_hook
    os.environ['NONCRITICAL_SLACK_WEBHOOK'] = noncritical_hook
    os.environ['BITLY_ACCESS_TOKEN'] = 'bitly-example-12345'

    metric_name = "HTTPCode_Target_5XX_Count"
    namespace = "AWS/ApplicationELB"
    dimensions = [
        {
            "name": "TargetGroup",
            "value": "targetgroup/api/e0aa3403356f01e9"
        },
        {
            "name": "LoadBalancer",
            "value": "app/api/e87a4a5f32874d8b"
        }
    ]
    timestamp = "2017-07-10T15:42:24.243+0000"

    alarm_info = {
        "AlarmName": alarm_name,
        "AlarmDescription": "This metric monitors api-alb-target-500-errors",
        "AWSAccountId": "account_id",
        "NewStateValue": "ALARM",
        "NewStateReason": alarm_reason,
        "StateChangeTime": timestamp,
        "Region": "EU - Ireland",
        "OldStateValue": "INSUFFICIENT_DATA",
        "Trigger": {
            "MetricName": metric_name,
            "Namespace": namespace,
            "StatisticType": "Statistic",
            "Statistic": "SUM",
            "Unit": "",
            "Dimensions": dimensions,
            "Period": 60,
            "EvaluationPeriods": 1,
            "ComparisonOperator": "GreaterThanOrEqualToThreshold",
            "Threshold": 1.0,
            "TreatMissingData": "",
            "EvaluateLowSampleCountPercentile": ""
        }
    }

    return {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventVersion': '1.0',
            'EventSubscriptionArn':
                'arn:aws:sns:region:account_id:alb_server_error_alarm:stuff',
            'Sns': {
                'Type': 'Notification',
                'MessageId': 'b20eb72b-ffc7-5d09-9636-e6f65d67d10f',
                'TopicArn':
                    'arn:aws:sns:region:account_id:alb_server_error_alarm',
                'Subject':
                    'ALARM: "api-alb-target-500-errors" in EU - Ireland',
                'Message': json.dumps(alarm_info),
                'Timestamp': '2017-07-10T15:42:24.307Z',
                'SignatureVersion': '1',
                'Signature': 'signature',
                'SigningCertUrl': 'https://certificate.pem',
                'UnsubscribeUrl': 'https://unsubscribe-url',
                'MessageAttributes': {}}
        }]
    }


@mock.patch('post_to_slack.requests.post')
def test_post_to_slack(
    mock_post, event, critical_hook, alarm_name, alarm_reason
):
    mock_post.return_value.ok = True

    post_to_slack.main(event, context=None)

    calls = mock_post.call_args_list

    assert len(calls) == 1
    assert calls[0][0][0] == critical_hook

    sent_data = json.loads(calls[0][1]['data'])

    assert len(sent_data['attachments']) == 1
    attachment = sent_data['attachments'][0]
    assert attachment['fallback'] == alarm_name
    assert attachment['title'] == alarm_name
    assert len(attachment['fields']) == 1
    assert attachment['fields'][0]['value'] == alarm_reason


class TestAlarm:

    @pytest.mark.parametrize('alarm_data, expected_reason', [
        (
            {
                'AlarmName': 'loris-alb-target-500-errors',
                'NewStateReason': 'Threshold Crossed: 1 datapoint [1.0 (11/08/18 10:55:00)] was greater than or equal to the threshold (1.0).',
            },
            'The ALB spotted a 500 error in Loris at 10:55:00 on 11 Aug 2018.'
        ),
        (
            {
                'AlarmName': 'api_romulus-alb-target-500-errors',
                'NewStateReason': 'Threshold Crossed: 1 datapoint [1.0 (11/08/18 10:55:00)] was greater than or equal to the threshold (1.0).',
            },
            'The ALB spotted a 500 error in the API at 10:55:00 on 11 Aug 2018.'
        ),
        (
            {
                'AlarmName': 'api_remus-alb-target-500-errors',
                'NewStateReason': 'Threshold Crossed: 1 datapoint [3.0 (11/08/18 10:55:00)] was greater than or equal to the threshold (1.0).',
            },
            'The ALB spotted multiple 500 errors (3) in the API at 10:55:00 on 11 Aug 2018.'
        ),
        (
            {
                'AlarmName': 'api_remus-alb-target-500-errors',
                'NewStateReason': 'Some other thing',
            },
            None
        ),
        (
            {
                'AlarmName': 'unrecognised-name',
                'NewStateReason': 'Threshold Crossed: 1 datapoint [3.0 (11/08/18 10:55:00)] was greater than or equal to the threshold (1.0).',
            },
            None
        ),
        (
            {
                'AlarmName': 'api_remus_v1-alb-not-enough-healthy-hosts',
                'NewStateReason': 'Threshold Crossed: no datapoints were received for 1 period and 1 missing datapoint was treated as [Breaching].',
            },
            "There are no healthy hosts in the ALB target group."
        ),
        (
            {
                'AlarmName': 'api_remus_v1-alb-unhealthy-hosts',
                'NewStateReason': 'Threshold Crossed: 1 datapoint [1.0 (09/01/18 10:23:00)] was greater than or equal to the threshold (1.0).',
            },
            "There is an unhealthy host in the API at 10:23:00 on 9 Jan 2018."
        ),
        (
            {
                'AlarmName': 'api_remus_v1-alb-unhealthy-hosts',
                'NewStateReason': 'Threshold Crossed: 1 datapoint [3.0 (10/02/19 10:26:00)] was greater than or equal to the threshold (1.0).',
            },
            "There are multiple unhealthy hosts (3) in the API at 10:26:00 on 10 Feb 2019."
        ),
        (
            {
                'AlarmName': 'api_romulus_v1-alb-not-enough-healthy-hosts',
                'NewStateReason': 'Threshold Crossed: 1 datapoint [0.0 (09/01/18 10:36:00)] was less than the threshold (0.0).',
            },
            "There aren't enough healthy hosts in the API (saw 0; expected more than 0) at 10:36:00 on 9 Jan 2018."
        ),
        (
            {
                'AlarmName': 'api_romulus_v1-alb-not-enough-healthy-hosts',
                'NewStateReason': 'Threshold Crossed: 1 datapoint [3.0 (09/01/18 10:36:00)] was less than the threshold (5.0).',
            },
            "There aren't enough healthy hosts in the API (saw 3; expected more than 5) at 10:36:00 on 9 Jan 2018."
        ),
    ])
    def test_human_reason(self, alarm_data, expected_reason):
        a = post_to_slack.Alarm(json.dumps(alarm_data))
        assert a.human_reason() == expected_reason


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
    assert post_to_slack.simplify_message(message) == expected


@pytest.mark.parametrize('name, is_critical', [
    ('lambda-notify_old_deploys-errors', False),
    ('api_remus_v1-alb-target-500-errors', True),
    ('api_remus_v2-alb-target-500-errors', True),
    ('es_ingest_queue_mel_dlq_not_empty', False),
    ('unknown_alarm', True),
])
def test_alarm_is_critical(name, is_critical):
    metadata = {'AlarmName': name}
    alarm = post_to_slack.Alarm(json.dumps(metadata))
    assert alarm.is_critical == is_critical
