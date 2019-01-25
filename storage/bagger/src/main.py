"""Listens to an SQS queue for b numbers to bag.

The message body is JSON, like this:

{
    "identifier": "b12345678",
    "do_not_bag": True
}

"""
import json
import bagger_processor
import aws
import status_table


def main():
    while True:
        messages = aws.get_bagging_messages()
        for message in messages:
            if message is not None:
                try:
                    body = json.loads(message.body)
                    bnumber = body["identifier"]
                    status_table.record_data(
                        bnumber,
                        {
                            "bagger_start": status_table.activity_timestamp(),
                            "bagger_batch_id": body["bagger_batch_id"],
                            "bagger_filter": body["bagger_filter"],
                        },
                    )
                    message.delete()
                    process_message(body)
                    status_table.record_activity(bnumber, "bagger_end")
                except Exception as e:
                    print("Unhandled exception {0}".format(e))


def process_message(message_body):
    identifier = message_body["identifier"]
    result = bagger_processor.process_bagging_message(message_body)
    error = result.get("error", None)
    if error is None:
        mets_only = message_body.get("do_not_bag", True)
        if not mets_only:
            # Only remove errors when a full bag has occurred
            aws.remove_error(identifier)
    else:
        print("Could not process {0}".format(identifier))
        aws.log_processing_error(result)


if __name__ == "__main__":
    main()
