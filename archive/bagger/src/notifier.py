import json
import logging

import aws
import settings


# result = {
#     "identifier": identifier,
#     "upload_location": None,
#     "duration": -1,
#     "error": None
# }

def _publish_notification(result):
    message = json.dumps(result)

    aws.publish(
        message=message,
        topic_arn=settings.BAGGING_COMPLETE_TOPIC_ARN
    )


def bagging_complete(result):
    logging.info(f"Bagging complete for {result['identifier']}")

    if result['upload_location']:
        _publish_notification(
            result['upload_location']
        )
