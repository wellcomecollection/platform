import boto3
import datetime
import settings


def get_table():
    dynamodb = boto3.resource("dynamodb", region_name=settings.AWS_DEFAULT_REGION)
    table = dynamodb.Table(settings.DYNAMO_TABLE)
    return table


def record_bagger_activity(bnumber, field):
    update_expression = "SET {0} = :bga".format(field)
    get_table().update_item(
        Key={"bnumber": bnumber},
        ExpressionAttributeValues={":bga": datetime.datetime.now().isoformat()},
        UpdateExpression=update_expression,
    )
