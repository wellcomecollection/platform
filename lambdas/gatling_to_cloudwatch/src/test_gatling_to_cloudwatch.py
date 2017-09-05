import boto3
from datetime import datetime
from moto import mock_cloudwatch
from mock import MagicMock

import gatling_to_cloudwatch

unix_time = 1501244573961
timestamp = datetime.fromtimestamp(unix_time / 1000)

assertions_json = f"""
{{
  "simulation": "testing.load.LorisSimulation",
  "simulationId": "lorissimulation",
  "start": {unix_time},
  "description": "",
  "scenarios": [
      "article-full-size",
      "random-full-size",
      "search-thumbnail",
      "complex"
  ],
  "assertions": [
    {{
      "path": "Global",
      "target": "max of response time",
      "condition": "is less than",
      "expectedValues": [1500.0],
      "result": false,
      "message": "Global: bar",
      "actualValue": [59645.0]
    }},
    {{
      "path": "Global",
      "target": "percentage of successful requests",
      "condition": "is greater than",
      "expectedValues": [95.0],
      "result": false,
      "message": "Global: foo",
      "actualValue": [50.77669902912621]
    }}
  ]
}}

"""

event = {
    'Records': [
        {
            'Sns': {
                'Message': assertions_json
            }
        }
    ]
}


@mock_cloudwatch
def test_send_assertions_to_cloudwatch():
    client = boto3.client('cloudwatch')
    client.put_metric_data = MagicMock(name='put_metric_data')

    gatling_to_cloudwatch.send_assertions_to_cloudwatch(
        client,
        event
    )

    client.put_metric_data.assert_called_once_with(
        MetricData=[
            {'MetricName': 'Global_max_of_response_time',
             'Timestamp': timestamp,
             'Value': 59645.0, 'Unit': 'Count'},
            {'MetricName': 'Global_percentage_of_successful_requests',
             'Timestamp': timestamp,
             'Value': 50.77669902912621, 'Unit': 'Count'}
        ],
        Namespace='gatling/testing.load.LorisSimulation'
    )
