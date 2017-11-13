import json
import os

import task_status_notifier

record = json.dumps({
    "ApproximateCreationDateTime": 1479499740,
    "Keys": {
        "task_definition_arn": {
            "S": "2016-11-18:12:09:36"
        },
        "task_arn": {
            "S": "John Doe"
        }
    },
    "NewImage": {
        "task_definition_arn": {
            "S": "2016-11-18:12:09:36"
        },
        "task_arn": {
            "S": "John Doe"
        },
        "started_at": {
            "S": "2016-11-18:12:09:36"
        },
        "completed": {
            "BOOL": True
        },
        "success": {
            "BOOL": False
        }
    },
    "SequenceNumber": "13021600000000001596893679",
    "SizeBytes": 112,
    "StreamViewType": "NEW_IMAGE"
})

event = {
    "Records": [
        {
            "Sns": {
                "Message": record
            }

        }
    ]
}


def test_task_status_notifier():
    os.environ["TASK_STOPPED_TOPIC_ARN"] = 'foo'
    os.environ["TASK_STARTED_TOPIC_ARN"] = 'foo'
    os.environ["TASK_UPDATED_TOPIC_ARN"] = 'foo'

    task_status_notifier.main(event, {})

    assert True is False
