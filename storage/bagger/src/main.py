"""Listens to an SQS queue for b numbers to bag.

The message body is JSON, like this:

{
    "identifier": "b12345678",
    "do_not_bag": True
}

"""
import json
import time
import bagger_processor
import aws


def main():
    while True:
        messages = aws.get_bagging_messages()
        for message in messages:
            if message is not None:
                try:
                    # THIS IS POTENTIALLY VERY LONG RUNNING
                    # LARGE BAGGING OPERATION, MINUTES OF I/O
                    print("-------")
                    # print(json.dumps(message, indent=4))
                    print(message)
                    process_message(message)
                except Exception as e:
                    print("Unhandled exception {0}".format(e))
                finally:
                    message.delete()


def process_message(message):
    start = time.time()
    body = json.loads(message.body)
    print(body)
    print("-------------------")
    identifier = body.get("identifier", "NO-IDENTIFIER")
    print("processing " + identifier)
    result = bagger_processor.process_bagging_message(body)
    error = result.get("error", None)
    if error is None:
        aws.remove_error(identifier)
    else:
        print("Could not process {0}".format(identifier))
        aws.log_processing_error(result)
    time_taken = time.time() - start
    print("{0} took {1} seconds".format(identifier, time_taken))


if __name__ == "__main__":
    main()
