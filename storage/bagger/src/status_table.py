import boto3
import datetime
import settings


def get_table():
    dynamodb = boto3.resource("dynamodb", region_name=settings.AWS_DEFAULT_REGION)
    table = dynamodb.Table(settings.DYNAMO_TABLE)
    return table


def record_bagger_activity(bnumber, field):
    record_bagger_data(bnumber, field, datetime.datetime.now().isoformat())


def record_bagger_data(bnumber, field, value):
    update_expression = "SET {0} = :v".format(field)
    get_table().update_item(
        Key={"bnumber": bnumber},
        ExpressionAttributeValues={":v": value},
        UpdateExpression=update_expression,
    )
