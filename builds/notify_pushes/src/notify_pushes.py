# -*- encoding: utf-8 -*-
"""
Sends Slack notifications for ECR pushes.
"""

import functools
import json
import os
import sys

from botocore.vendored import requests


def log_event_on_error(handler):
    @functools.wraps(handler)
    def wrapper(event, context):
        try:
            return handler(event, context)
        except Exception:
            print("event = %r" % event)
            raise

    return wrapper


@log_on_error
def main(event, context):
    slack_webhook = os.environ["SLACK_WEBHOOK"]

    message = event["Records"][0]["Sns"]

    data = json.loads(message["Message"])

    iam_user = data["iam_user"]
    project = data["project"]
    git_branch = data["git_branch"]
    commit_id = data["commit_id"]
    commit_msg = data["commit_msg"].splitlines()[0]

    if data["push_type"] == "aws_lambda":
        name = project.replace(".zip", "")
        message = f"{iam_user} has published a new *{name}* ZIP to S3."
        username = "lambda-pushes"
        icon_emoji = "lambda"
    else:
        message = f"{iam_user} has pushed a new *{project}* image to ECR."
        username = "ecr-pushes"
        icon_emoji = "ecr"

    lines = [message]

    if (git_branch != "HEAD") and (commit_id != "HEAD"):
        lines.append(f"Git branch: {git_branch} ({commit_id})")

    lines.append(f"Commit message: {commit_msg!r}")

    slack_data = {
        "username": username,
        "icon_emoji": f":{icon_emoji}:",
        "attachments": [{"color": "#F58536", "fields": [{"value": l} for l in lines]}],
    }

    print(f"Sending message {slack_data!r}")

    response = requests.post(
        slack_webhook,
        data=json.dumps(slack_data),
        headers={"Content-Type": "application/json"},
    )
    response.raise_for_status()
