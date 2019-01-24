import json
import logging

import aws
import settings


def _publish_notification(result):
    message = json.dumps(result)

    if settings.BAGGING_COMPLETE_TOPIC_ARN:
        aws.publish(message=message, topic_arn=settings.BAGGING_COMPLETE_TOPIC_ARN)


def bagging_complete(result):
    logging.info(
        "-- completed bagging %s in %s secs",
        result["identifier"],
        "{0:.1f}".format(result["duration"]),
    )
    _publish_notification(result)
