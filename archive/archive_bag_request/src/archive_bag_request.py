# -*- encoding: utf-8 -*-

import attr
import os
import boto3
from wellcome_aws_utils.lambda_utils import log_on_error
from wellcome_aws_utils.sns_utils import publish_sns_message


@log_on_error
def main(event=None, _ctxt=None, sns_client=None):
    print(event)
    print(os.environ)

    sns_client = sns_client or boto3.client('sns')

    return False
