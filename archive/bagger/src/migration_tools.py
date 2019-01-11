import fire
import aws
import time
import datetime
import dateutil
import boto3
import settings
import requests
import json
import storage_api
from mets_filesource import b_numbers_from_s3


def json_default(o):
    if isinstance(o, datetime.datetime):
        return o.isoformat()


class MigrationTool(object):
    def populate_initial(self, filter=""):
        populate_initial(filter)

    def update_status(self, delay, filter=""):
        update_bag_and_ingest_status(delay, filter)

    def ingest(self, delay, filter=""):
        do_ingest(delay, filter)

    def simulate_goobi_call(self, delay, filter=""):
        call_dds(delay, filter)


def populate_initial(filter):
    table = get_table()
    chunk_counter = 0
    b_counter = 0
    for chunk in batch(bnumber_generator(filter)):
        chunk_counter = chunk_counter + 1
        print("...inserting chunk {0}".format(chunk_counter))
        with table.batch_writer() as db_batch:
            for bnumber in chunk:
                b_counter = b_counter + 1
                db_batch.put_item(Item={"bnumber": bnumber})
    print("FINISHED, inserted {0} b numbers in {1} chunks.", b_counter, chunk_counter)


def get_min_bag_date():
    min_bag_date = datetime.datetime(2018, 12, 1)
    try:
        min_bag_date = dateutil.parser.parse(settings.MINIMUM_BAG_DATE)
    except ValueError:
        print("no parseable bag cutoff date in " + settings.MINIMUM_BAG_DATE)
    return min_bag_date


def update_bag_and_ingest_status(delay, filter):
    table = get_table()
    no_ingest = {
        "id": "-",
        "status": {"id": "no-ingest"},
        "events": [{"createdDate": "-"}],
    }

    print("[")

    for bnumber in bnumber_generator(filter):
        try:
            output = update_bag_and_ingest_status_bnumber(bnumber, table, no_ingest)
        except Exception as e:
            err_obj = {"ERROR": bnumber, "error": e}
            print(err_obj)
            raise
        print(json.dumps(output, default=json_default, indent=4))
        print(",")
        if delay > 0:
            time.sleep(delay)

    print("]")


def update_bag_and_ingest_status_bnumber(bnumber, table, no_ingest):
    bag_date = "-"
    bag_size = 0
    bag_error = "-"

    # check for bag
    bag_zip = aws.get_dropped_bag_info(bnumber)
    if bag_zip["exists"]:  # and bag_zip["last_modified"] > min_bag_date:
        bag_date = bag_zip["last_modified"].isoformat()
        bag_size = bag_zip["size"]
    else:
        error_obj = aws.get_error_for_b_number(bnumber)
        if error_obj is not None:
            message = error_obj["error"].splitlines()[-1]
            last_modified = error_obj["last_modified"]
            bag_error = "{0} {1}".format(last_modified, message)

    ingest = storage_api.get_ingest_for_identifier(bnumber)
    if ingest is None:
        ingest = no_ingest

    table.update_item(
        Key={"bnumber": bnumber},
        ExpressionAttributeValues={
            ":bdt": bag_date,
            ":bsz": bag_size,
            ":bge": bag_error,
            ":idt": ingest["events"][0]["createdDate"],
            ":iid": ingest["id"],
            ":ist": ingest["status"]["id"],
        },
        UpdateExpression="SET bag_date = :bdt, bag_size = :bsz, mets_error = :bge, ingest_date = :idt, ingest_id = :iid, ingest_status = :ist",
    )
    return {
        "identifier": bnumber,
        "bag_zip": bag_zip,
        "mets_error": bag_error,
        "ingest": ingest,
    }


def bnumber_generator(filter_expression):
    if filter_expression.startswith("b"):
        return (b for b in [filter_expression])
    else:
        return b_numbers_from_s3(filter_expression)


def get_table():
    dynamodb = boto3.resource("dynamodb", region_name=settings.AWS_DEFAULT_REGION)
    table = dynamodb.Table(settings.DYNAMO_TABLE)
    return table


def batch(bnumbers):
    chunk = []
    counter = 1
    for b in bnumbers:
        chunk.append(b)
        if counter == 25:
            yield chunk
            chunk = []
            counter = 1
        else:
            counter = counter + 1
    if len(chunk) > 0:
        yield chunk


def empty_item(bnumber):
    return {
        "bnumber": bnumber,
        "bag_date": None,
        "ingest_started": None,
        "ingest_id": None,
        "ingest_status": None,
    }


def do_ingest(delay, filter):
    print("[")
    for bnumber in bnumber_generator(filter):
        ingest = storage_api.ingest(bnumber)
        print(json.dumps(ingest, default=json_default, indent=4))
        print(",")
        if delay > 0:
            time.sleep(delay)

    print('"end"')
    print("]")


def call_dds(delay, filter):
    table = get_table()
    for bnumber in bnumber_generator(filter):
        now = datetime.datetime.now().isoformat
        print("[")
        url = settings.DDS_GOOBI_NOTIFICATION.format(bnumber)
        r = requests.get(url)
        j = r.json()
        print(json.dumps(j, indent=4))
        print(",")

        # now update the dynamodb record
        table.update_item(
            Key={"bnumber": bnumber},
            ExpressionAttributeValues={":dc": now},
            UpdateExpression="SET dds_called = :dc",
        )

        if delay > 0:
            time.sleep(delay)

    print('{ "finished": "' + datetime.datetime.now().isoformat + '" }')
    print("]")


if __name__ == "__main__":
    fire.Fire(MigrationTool)
