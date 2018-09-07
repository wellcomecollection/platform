import json
import time

"""
    Listens to a queue for b numbers to bag.
    The message body is JSON, like this:
    
    {
        "identifier": "b12345678",
        "do_not_bag": True
    }

    
"""
def main():
    # get message from queue
    # bag or no bag
    # write result to queue
    # TODO - this needs to use local mock SQS/SNS
    print("...be given a boto3 sqs client via some form of injection")
    sample_message =     {
        "identifier": "b12345678",
        "do_not_bag": True
    }
    print("...fetch messages from the queue that look like this:")
    print()
    print(json.dumps(sample_message, indent=4))
    print()
    while 1:
        print("polling...")
        time.sleep(10)


if __name__ == "__main__":
    main()