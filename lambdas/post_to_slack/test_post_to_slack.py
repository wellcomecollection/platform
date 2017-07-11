import json
import os
import pprint

import post_to_slack
from mock import patch


def _assert_field_contains(field, title, value):
    assert field['title'] == title
    assert field['value'] == value


@patch('post_to_slack.requests.post')
def test_post_to_slack(mock_post):
    url = "http://blah.com"
    os.environ['SLACK_INCOMING_WEBHOOK'] = url
    mock_post.return_value.ok = True

    alarm_name = "api-alb-target-500-errors"
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
    reason = "Threshold Crossed: 1 datapoint (4.0) was \
        greater than or equal to the threshold (1.0)."
    timestamp = "2017-07-10T15:42:24.243+0000"

    alarm_info = {
        "AlarmName": alarm_name,
        "AlarmDescription": "This metric monitors api-alb-target-500-errors",
        "AWSAccountId": "account_id",
        "NewStateValue": "ALARM",
        "NewStateReason": reason,
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

    event = {
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

    post_to_slack.main(event, None)

    calls = mock_post.call_args_list

    assert len(calls) == 1

    assert calls[0][0][0] == url

    sent_data = json.loads(calls[0][1]['data'])

    assert len(sent_data['attachments']) == 1
    attachment = sent_data['attachments'][0]
    assert attachment['fallback'] == alarm_name
    assert attachment['title'] == alarm_name
    assert len(attachment['fields']) == 4

    _assert_field_contains(
        field=attachment['fields'][0],
        title='Metric',
        value=f'{namespace}/{metric_name}'
    )
    _assert_field_contains(
        field=attachment['fields'][1],
        title='Dimensions',
        value=f'{pprint.pformat(dimensions)}'
    )
    _assert_field_contains(
        field=attachment['fields'][2],
        title='Reason',
        value=reason
    )
    _assert_field_contains(
        field=attachment['fields'][3],
        title='Timestamp',
        value=timestamp
    )
