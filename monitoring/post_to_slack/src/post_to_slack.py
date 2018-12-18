# -*- encoding: utf-8 -*-
"""
Sends slack notifications for alarms events
"""

import datetime as dt
import json
import os

import attr
import boto3
import requests
from wellcome_aws_utils.lambda_utils import log_on_error

from cloudwatch_alarms import (
    build_cloudwatch_url,
    datetime_to_cloudwatch_ts,
    ThresholdMessage,
)
from platform_alarms import (
    get_human_message,
    guess_cloudwatch_log_group,
    guess_cloudwatch_search_terms,
    is_critical_error,
    simplify_message,
)


@attr.s
class Interval:
    start = attr.ib()
    end = attr.ib()


class MessageHasNoDateError(Exception):
    pass


@attr.s
class Alarm:
    message = attr.ib(converter=json.loads)

    @property
    def name(self):
        return self.message["AlarmName"]

    @property
    def state_reason(self):
        return self.message["NewStateReason"]

    @property
    def should_be_sent_to_main_channel(self):
        return self.name not in [
            "lambda-reporting_miro_transformer-errors",
            "lambda-reporting_miro_inventory_transformer-errors",
            "lambda-reporting_sierra_transformer-errors",
        ]

    # Sometimes there's enough data in the alarm to make an educated guess
    # about useful CloudWatch logs to check, so we include that in the alarm.
    # The methods and properties below pull out the relevant info.

    def cloudwatch_timeframe(self):
        """
        Try to work out a likely timeframe for CloudWatch errors.
        """
        threshold = ThresholdMessage.from_message(self.state_reason)

        try:
            return Interval(
                start=threshold.date - dt.timedelta(seconds=300),
                end=threshold.date + dt.timedelta(seconds=300),
            )
        except TypeError:
            # Raised when threshold.date is None.
            raise MessageHasNoDateError()

    def cloudwatch_urls(self):
        """
        Return some CloudWatch URLs that might be useful to check.
        """
        try:
            log_group_name = guess_cloudwatch_log_group(alarm_name=self.name)
            timeframe = self.cloudwatch_timeframe()
            return [
                build_cloudwatch_url(
                    search_term=search_term,
                    log_group_name=log_group_name,
                    start_date=timeframe.start,
                    end_date=timeframe.end,
                )
                for search_term in guess_cloudwatch_search_terms(alarm_name=self.name)
            ]

        except MessageHasNoDateError:
            pass

        except ValueError as err:
            print(f"Error in cloudwatch_urls: {err}")
            return []

    def cloudwatch_messages(self):
        """
        Try to find some CloudWatch messages that might be relevant.
        """
        client = boto3.client("logs")

        messages = []

        try:
            log_group_name = guess_cloudwatch_log_group(alarm_name=self.name)

            # CloudWatch wants these parameters specified as seconds since
            # 1 Jan 1970 00:00:00, so convert to that first.
            timeframe = self.cloudwatch_timeframe()
            startTime = datetime_to_cloudwatch_ts(timeframe.start)
            endTime = datetime_to_cloudwatch_ts(timeframe.end)

            # We only get the first page of results.  If there's more than
            # one page, we have so many errors that not getting them all
            # in the Slack alarm is the least of our worries!
            for term in guess_cloudwatch_search_terms(alarm_name=self.name):
                resp = client.filter_log_events(
                    logGroupName=log_group_name,
                    startTime=startTime,
                    endTime=endTime,
                    filterPattern=term,
                )
                messages.extend([e["message"] for e in resp["events"]])

        except MessageHasNoDateError:
            pass

        except Exception as err:
            print(f"Error in cloudwatch_messages: {err!r}")

        return messages


def to_bitly(sess, url, access_token):
    """
    Try to shorten a URL with bit.ly.  If it fails, just return the
    original URL.
    """
    resp = sess.get(
        "https://api-ssl.bitly.com/v3/user/link_save",
        params={"access_token": access_token, "longUrl": url},
    )
    try:
        return resp.json()["data"]["link_save"]["link"]
    except TypeError:  # thrown if "data" = null
        print(f"response from bit.ly: {resp.json()}")
        return url


def prepare_slack_payload(alarm, bitly_access_token, sess=None):
    if is_critical_error(alarm_name=alarm.name):
        slack_data = {"username": "cloudwatch-alarm", "icon_emoji": ":rotating_light:"}
        alarm_color = "danger"
    else:
        slack_data = {"username": "cloudwatch-warning", "icon_emoji": ":warning:"}
        alarm_color = "warning"

    slack_data["attachments"] = [
        {
            "color": alarm_color,
            "fallback": alarm.name,
            "title": alarm.name,
            "fields": [
                {
                    "value": get_human_message(
                        alarm_name=alarm.name, state_reason=alarm.state_reason
                    )
                }
            ],
        }
    ]

    messages = alarm.cloudwatch_messages()
    if messages:
        cloudwatch_message_str = "\n".join(set([simplify_message(m) for m in messages]))
        slack_data["attachments"][0]["fields"].append(
            {"title": "CloudWatch messages", "value": cloudwatch_message_str}
        )

    cloudwatch_urls = alarm.cloudwatch_urls()
    if cloudwatch_urls:
        sess = sess or requests.Session()
        cloudwatch_url_str = " / ".join(
            [
                to_bitly(sess=sess, url=url, access_token=bitly_access_token)
                for url in cloudwatch_urls
            ]
        )
        slack_data["attachments"][0]["fields"].append({"value": cloudwatch_url_str})

    return slack_data


@log_on_error
def main(event, _ctxt=None):
    bitly_access_token = os.environ["BITLY_ACCESS_TOKEN"]
    alarm = Alarm(event["Records"][0]["Sns"]["Message"])

    if alarm.should_be_sent_to_main_channel:
        webhook_url = os.environ["CRITICAL_SLACK_WEBHOOK"]
    else:
        webhook_url = os.environ["NONCRITICAL_SLACK_WEBHOOK"]

    slack_data = prepare_slack_payload(alarm, bitly_access_token)

    print("Sending message %s" % json.dumps(slack_data))

    response = requests.post(
        webhook_url,
        data=json.dumps(slack_data),
        headers={"Content-Type": "application/json"},
    )
    response.raise_for_status()
