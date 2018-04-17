# -*- encoding: utf-8 -*-

import datetime as dt
import json
import os

from betamax import Betamax
import boto3
import mock
import moto
import pytest
import requests

from cloudwatch_alarms import datetime_to_cloudwatch_ts
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

    post_to_slack.main(event)

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


with Betamax.configure() as config:
    config.cassette_library_dir = '.'

    access_token = os.environ.get('BITLY_ACCESS_TOKEN', 'testtoken')
    config.define_cassette_placeholder('<ACCESS_TOKEN>', access_token)


@pytest.fixture
def sess():
    session = requests.Session()
    with Betamax(session) as vcr:
        vcr.use_cassette('test_post_to_slack')
        yield session


class TestPrepareSlackPayload:

    @moto.mock_logs
    def test_critical_is_alarm(self, sess):
        alarm = post_to_slack.Alarm(json.dumps({
            'AlarmName': 'api_remus_v1-alb-target-500-errors',
            'NewStateReason': 'Threshold Crossed: 1 datapoint [1.0 (01/01/01 12:00:00)] was greater than or equal to the threshold (1.0).',
        }))
        payload = post_to_slack.prepare_slack_payload(
            alarm=alarm, bitly_access_token=access_token, sess=sess
        )

        assert payload['username'] == 'cloudwatch-alarm'
        assert payload['icon_emoji'] == ':rotating_light:'
        assert payload['attachments'][0]['color'] == 'danger'

    @moto.mock_logs
    def test_non_critical_is_warning(self, sess):
        alarm = post_to_slack.Alarm(json.dumps({
            'AlarmName': 'sierra_bibs_merger_queue_dlq_not_empty',
            'NewStateReason': 'Threshold Crossed: 1 datapoint [1.0 (01/01/01 12:00:00)] was greater than or equal to the threshold (1.0).',
        }))
        payload = post_to_slack.prepare_slack_payload(
            alarm=alarm, bitly_access_token=access_token, sess=sess
        )

        assert payload['username'] == 'cloudwatch-warning'
        assert payload['icon_emoji'] == ':warning:'
        assert payload['attachments'][0]['color'] == 'warning'

    @pytest.mark.skip(
        reason="filter_log_events() isn't implemented in moto"
    )
    @moto.mock_logs
    def test_including_cloudwatch_messages(self, sess):
        # Populate the CloudWatch log stream with a bunch of logs, then
        # we'll check we get something broadly sensible at the end.
        client = boto3.client('logs')
        client.create_log_group(logGroupName='platform/loris')
        client.create_log_stream(
            logGroupName='platform/loris',
            logStreamName='logstream001'
        )

        def _event(minute, status_code):
            return {
                'timestamp': datetime_to_cloudwatch_ts(dt.datetime(2000, 1, 1, 12, minute, 0, 0)),
                'message': f'[Mon Jan 1 12:{minute}:00 2001] GET /V0{status_code}.jpg/full/300,/0/default.jpg => generated 1000 bytes in 10 msecs (HTTP/1.0 {status_code})'
            }

        client.put_log_events(
            logGroupName='platform/loris',
            logStreamName='logstream001',
            logEvents=[
                _event(minute=minute, status_code=status_code)
                for minute, status_code in enumerate([
                    200, 200, 500, 200, 500, 500, 200, 500, 200, 200, 500, 200
                ], start=22)
            ]
        )

        alarm = post_to_slack.Alarm(json.dumps({
            'AlarmName': 'loris-alb-target-500-errors',
            'NewStateReason': 'Threshold Crossed: 1 datapoint [4.0 (01/01/01 12:00:00)] was greater than or equal to the threshold (1.0).',
        }))

        post_to_slack.prepare_slack_payload(
            alarm=alarm, bitly_access_token=access_token, sess=sess
        )

        # TODO: Make an assertion on the payload about the log events we see.


@mock.patch('post_to_slack.requests.post')
def test_regression_issue_1900(mock_post):
    event = {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventSubscriptionArn': 'arn:aws:sns:eu-west-1:760097843905:shared_alb_server_error_alarm:7701d498-64a4-425e-b966-f9069ecf91a2',
            'EventVersion': '1.0',
            'Sns': {
                'Message': '{"AlarmName":"loris-alb-not-enough-healthy-hosts","AlarmDescription":"This '
                           'metric monitors '
                           'loris-alb-not-enough-healthy-hosts","AWSAccountId":"760097843905","NewStateValue":"ALARM","NewStateReason":"Threshold '
                           'Crossed: no datapoints were received for 1 '
                           'period and 1 missing datapoint was treated '
                           'as '
                           '[Breaching].","StateChangeTime":"2018-04-16T15:34:56.438+0000","Region":"EU '
                           '(Ireland)","OldStateValue":"INSUFFICIENT_DATA","Trigger":{"MetricName":"HealthyHostCount","Namespace":"AWS/ApplicationELB","StatisticType":"Statistic","Statistic":"SUM","Unit":null,"Dimensions":[{"name":"TargetGroup","value":"targetgroup/loris/087013874ebbe481"},{"name":"LoadBalancer","value":"app/loris/c0af06f31b54a8c9"}],"Period":60,"EvaluationPeriods":1,"ComparisonOperator":"LessThanThreshold","Threshold":2.0,"TreatMissingData":"- '
                           'TreatMissingData: '
                           'Breaching","EvaluateLowSampleCountPercentile":""}}',
                'MessageAttributes': {},
                'MessageId': '6bd3a539-6665-5449-9496-f677a285954e',
                'Signature': 'QI0mjffHTGE0y/HcQQoECquP0c3Xtzc6CiFzFBEfM3KRKWFmT2gcebnRU7vGBqh5uHKU+H98k7XJkUqkTVwtz0NkNk5gnRdP/Aqz3ZOtheRqv2qydbzXiIh94mHm9u62Fsiplqlq2vXdKMRKtRFovw58baieYSYAuPhxf+s74TTIY1gU79Lx4IdXfJHf1xfT2UZLHvt83swy5C+19elIlqM9+48+dnShRdQJAk/SOQnGNyTWeVdu+6w2JeNNa6avFaTqLryIBXUO8kgQy6E5VdXtka0Ufiu6qmDKhWRWZajGQBhYHpS6QbgVD7tsBr+FEgeQSb8JfOeuAHtly+hIOw==',
                'SignatureVersion': '1',
                'SigningCertUrl': 'https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-433026a4050d206028891664da859041.pem',
                'Subject': 'ALARM: "loris-alb-not-enough-healthy-hosts" '
                           'in EU (Ireland)',
                'Timestamp': '2018-04-16T15:34:56.473Z',
                'TopicArn': 'arn:aws:sns:eu-west-1:760097843905:shared_alb_server_error_alarm',
                'Type': 'Notification',
                'UnsubscribeUrl': 'https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:760097843905:shared_alb_server_error_alarm:7701d498-64a4-425e-b966-f9069ecf91a2'
            }
        }]
    }

    mock_post.return_value.ok = True
    post_to_slack.main(event)
