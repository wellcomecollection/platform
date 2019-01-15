import os
import json
import boto3
import settings
from botocore.exceptions import ClientError

boto_session_platform = None
boto_session_storage = None
sns_client = boto3.client("sns")


def publish(message, topic_arn):
    return sns_client.publish(Message=message, TopicArn=topic_arn)


def get_boto_session_storage():
    global boto_session_storage
    if boto_session_storage is None:
        boto_session_storage = boto3.Session(region_name=settings.AWS_DEFAULT_REGION, profile_name="storage")
    return boto_session_storage


def get_boto_session_platform():
    global boto_session_platform
    if boto_session_platform is None:
        boto_session_platform = boto3.Session(region_name=settings.AWS_DEFAULT_REGION, profile_name="platform")
    return boto_session_platform


def get_s3():
    # The python migration code never needs to talk to storage profile
    # buckets, only the platform buckets
    return get_boto_session_platform().resource("s3")


def upload(source, key):
    client = get_boto_session_platform().client("s3")
    client.upload_file(source, settings.DROP_BUCKET_NAME, key)
    return {"bucket": settings.DROP_BUCKET_NAME, "key": key}


def get_mets_xml(s3_path):
    obj = get_s3().Object(settings.METS_BUCKET_NAME, s3_path)
    xml_string = obj.get()["Body"].read().decode("utf-8")
    return xml_string


def save_mets_to_side(b_number, local_tmp_file):
    client = get_boto_session_platform().client("s3")
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
    sqs = get_boto_session_storage().resource("sqs")
    queue = None
    try:
        queue = sqs.get_queue_by_name(QueueName=settings.BAGGING_QUEUE)
    except ClientError as ce:
        if ce.response["Error"]["Code"] == "AWS.SimpleQueueService.NonExistentQueue":
            print("I won't make the queue on demand any more...")
            raise
            # queue = sqs.create_queue(
            #     QueueName=settings.BAGGING_QUEUE, Attributes={"DelaySeconds": "0"}
            # )
            # print("Created queue - " + settings.BAGGING_QUEUE)

    response = queue.send_message(MessageBody=json.dumps(message))
    return response


def get_bagging_messages():
    sqs = get_boto_session_storage().resource("sqs")
    queue = sqs.get_queue_by_name(QueueName=settings.BAGGING_QUEUE)
    return queue.receive_messages(WaitTimeSeconds=settings.POLL_INTERVAL)


def get_error_for_b_number(bnumber):
    try:
        obj = get_s3().Object(settings.DROP_BUCKET_NAME_ERRORS, bnumber + ".json")
        content = obj.get()["Body"].read().decode("utf-8")
        b_error = json.loads(content)
        b_error["last_modified"] = str(obj.last_modified)
        return b_error
    except ClientError as e:
        if e.response["Error"]["Code"] == "NoSuchKey":
            return None
        raise


def get_all_errors():
    bucket = get_s3().Bucket(settings.DROP_BUCKET_NAME_ERRORS)
    for error in bucket.objects.all():
        bnumber = error.key.split(".")[-2]
        yield get_error_for_b_number(bnumber)


def remove_error(bnumber):
    obj = get_s3().Object(settings.DROP_BUCKET_NAME_ERRORS, bnumber + ".json")
    obj.delete()


def get_dropped_bag_info(bnumber):
    key = "{0}.zip".format(bnumber)
    bag_info = {"exists": False}
    try:
        client = get_boto_session_platform().client("s3")
        bag_head = client.head_object(Bucket=settings.DROP_BUCKET_NAME, Key=key)
    except ClientError as e:
        if e.response["Error"]["Code"] == "404":
            return bag_info
        else:
            raise  # Something else has gone wrong.
    else:
        bag_info["exists"] = True
        bag_info["last_modified"] = bag_head["LastModified"]
        bag_info["size"] = bag_head["ContentLength"]

    return bag_info
