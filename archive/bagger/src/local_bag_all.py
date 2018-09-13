import sys
import time
import logging
import bagger_processor
import aws
from mets_filesource import b_numbers_from_s3


def main():
    if len(sys.argv) != 3:
        print("usage: local_bag_all.py <filter> <bag|no-bag>")
        print("e.g.,  local_bag_all.py x/1/2 no-bag")
    else:
        filter = sys.argv[1]
        skip = sys.argv[2] != "bag"
        start = time.time()
        counter = 0
        for b_number in b_numbers_from_s3(filter):
            logging.info("processing " + b_number)
            message = {
                "identifier": b_number,
                "do_not_bag": skip
            }
            result = bagger_processor.process_bagging_message(message)
            error = result.get("error", None)
            if error is not None:
                aws.log_processing_error(result)
            counter = counter + 1
        time_taken = time.time() - start
        print("----------------")
        print("{0} items in {1} seconds.".format(counter, time_taken))


if __name__ == "__main__":
    main()
