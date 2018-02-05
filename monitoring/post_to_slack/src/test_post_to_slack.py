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
    noncritical_hook = 'https://api.slack.com/hooks/example_critical'

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
def test_post_to_slack(mock_post, event, critical_hook, alarm_name):
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
    assert (
        attachment['fields'][0]['value'] ==
        'There was a 500 error from the api ALB target group.')
