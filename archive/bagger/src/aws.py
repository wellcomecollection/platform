import os
import json
import boto3
import settings
from botocore.exceptions import ClientError

boto_session = None


def get_boto_session():
    global boto_session
    if boto_session is None:
        boto_session = boto3.Session(region_name=settings.AWS_DEFAULT_REGION)
    return boto_session


def get_s3():
    return get_boto_session().resource("s3")


def upload(source, key):
    client = get_boto_session().client("s3")
    client.upload_file(source, settings.DROP_BUCKET_NAME, key)


def get_mets_xml(s3_path):
    obj = get_s3().Object(settings.METS_BUCKET_NAME, s3_path)
    xml_string = obj.get()["Body"].read().decode("utf-8")
    return xml_string


def save_mets_to_side(b_number, local_tmp_file):
    client = get_boto_session().client("s3")
    key = "{0}/{1}".format(b_number, os.path.basename(local_tmp_file))
    client.upload_file(local_tmp_file, settings.DROP_BUCKET_NAME_METS_ONLY, key)


def save_id_map(b_number, id_map):
    s3_path = "_idmaps/{0}.json".format(b_number)
    obj = get_s3().Object(settings.DROP_BUCKET_NAME_METS_ONLY, s3_path)
    obj.put(Body=json.dumps(id_map, indent=4))


def log_processing_error(message):
    s3_path = "{0}.json".format(message["identifier"])
    obj = get_s3().Object(settings.DROP_BUCKET_NAME_ERRORS, s3_path)
    obj.put(Body=json.dumps(message, indent=4))


def send_bag_instruction(message):
    sqs = get_boto_session().resource("sqs")
    queue = None
    try:
        queue = sqs.get_queue_by_name(QueueName=settings.BAGGING_QUEUE)
    except ClientError as ce:
        if ce.response["Error"]["Code"] == "AWS.SimpleQueueService.NonExistentQueue":
            queue = sqs.create_queue(
                QueueName=settings.BAGGING_QUEUE, Attributes={"DelaySeconds": "0"}
            )
            print("Created queue - " + settings.BAGGING_QUEUE)

    response = queue.send_message(MessageBody=json.dumps(message))
    return response


def get_bagging_messages():
    sqs = get_boto_session().resource("sqs")
    queue = sqs.get_queue_by_name(QueueName=settings.BAGGING_QUEUE)
    return queue.receive_messages(WaitTimeSeconds=settings.POLL_INTERVAL)


def get_error_for_b_number(bnumber):
    obj = get_s3().Object(settings.DROP_BUCKET_NAME_ERRORS, bnumber + ".json")
    content = obj.get()["Body"].read().decode("utf-8")
    return json.loads(content)


def get_all_errors():
    bucket = get_s3().Bucket(settings.DROP_BUCKET_NAME_ERRORS)
    for error in bucket.objects.all():
        bnumber = error.key.split(".")[-2]
        yield get_error_for_b_number(bnumber)


def remove_error(bnumber):
    obj = get_s3().Object(settings.DROP_BUCKET_NAME_ERRORS, bnumber + ".json")
    obj.delete()
