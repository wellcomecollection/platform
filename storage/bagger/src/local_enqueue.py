"""Add b numbers to the queue from the local machine.

This is the primary means of triggering the process, as it is a manual decision.

USAGE:

local_enqueue.py <filter|bnumber> <bag|no-bag>


the <filter> limits the b numbers returned to a filtered set, based on keys.
The spread of b numbers is fairly even.

EXAMPLES:

local_enqueue.py x/1/2 no-bag
Enqueue approximately 0.1% of the b numbers, but do not bag - just process METS

local_enqueue.py b12345678 bag
Enqueue one b number only, and bag it.

"""

import sys
import time
import logging
import aws
import uuid
from mets_filesource import bnumber_generator


def main():
    if len(sys.argv) != 3:
        print("usage: local_enqueue.py <filter|bnumber> <bag|no-bag>")
        print("e.g.,  local_enqueue.py x/1/2 no-bag")
        print("e.g.,  local_enqueue.py b12345678 bag")
    else:
        to_process = sys.argv[1]
        skip = sys.argv[2] != "bag"
        info = "for bagging"
        if skip:
            info = "METS only"
        start = time.time()
        counter = 0

        batch_id = str(uuid.uuid4())

        for b_number in bnumber_generator(to_process):
            counter = counter + 1
            logging.debug("processing " + b_number)
            message = {
                "identifier": b_number,
                "bagger_batch_id": batch_id,
                "bagger_filter": to_process,
                "do_not_bag": skip,
            }
            print("{0}: enqueueing {1}, {2}".format(counter, b_number, info))
            response = aws.send_bag_instruction(message)
            message["MessageId"] = response.get("MessageId", "NO-MESSAGE-ID")
            print(message)
            print("-------------------")

        time_taken = time.time() - start
        print("{0} items in {1} seconds.".format(counter, time_taken))


if __name__ == "__main__":
    main()
