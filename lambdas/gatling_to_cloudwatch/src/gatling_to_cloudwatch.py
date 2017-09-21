import boto3
from datetime import datetime
import json


def _send_metric(client, namespace, metric_data):
    print(f'sending metric for {namespace}: {metric_data!r}')

    return client.put_metric_data(
        Namespace=namespace,
        MetricData=metric_data
    )


def _generate_metric(assertion, timestamp):
    target = assertion['target'].replace(" ", "_")
    path = assertion['path'].replace(" ", "_")
    value = assertion['actualValue'][0]

    metric_name = f'{path}_{target}'

    return {
        'MetricName': metric_name,
        'Timestamp': timestamp,
        'Value': value,
        'Unit': 'Count'
    }


def _extract_sns_message(event):
    message = event['Records'][0]['Sns']['Message']
    return json.loads(message)


def send_assertions_to_cloudwatch(client, event):
    message_data = _extract_sns_message(event)

    simulation = message_data['simulation']
    assertions = message_data['assertions']
    start_time = message_data['start'] / 1000

    namespace = f'gatling/{simulation}'
    timestamp = datetime.fromtimestamp(start_time)
    metric_data = (
        [_generate_metric(a, timestamp) for a in assertions]
    )

    resp = _send_metric(
        client=client,
        namespace=namespace,
        metric_data=metric_data
    )
    print(f'resp = {resp}')


def main(event, _):
    print(f'event = {event!r}')

    client = boto3.client('cloudwatch')
    send_assertions_to_cloudwatch(client, event)
