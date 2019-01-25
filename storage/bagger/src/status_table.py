import boto3
import datetime
import settings


def get_table():
    dynamodb = boto3.resource("dynamodb", region_name=settings.AWS_DEFAULT_REGION)
    table = dynamodb.Table(settings.DYNAMO_TABLE)
    return table


def record_activity(bnumber, field):
    record_data(bnumber, {field: activity_timestamp()})


def activity_timestamp():
    datetime.datetime.now().isoformat()


def record_data(bnumber, data):
    placeholders = [f":v{x+1}" for x in range(len(data))]
    key_to_placeholder = dict(zip(data.keys(), placeholders))
    expression_attribute_values = dict((key_to_placeholder[key], value) for (key, value) in data.items())
    placeholder_updates = ", ".join({f"{k}={p}" for k,p in key_to_placeholder.items()})

    get_table().update_item(
        Key={"bnumber": bnumber},
        ExpressionAttributeValues=expression_attribute_values,
        UpdateExpression=f"SET {placeholder_updates}",
    )
