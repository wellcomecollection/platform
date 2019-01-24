"""Run the bagging process on the local machine (i.e., not from a queue)

USAGE
local_bag_all.py <filter> <bag|no-bag>
e.g.,  local_bag_all.py x/1/2 no-bag

The filter limits the b numbers returned to a filtered set, based on keys.
The spread of b numbers is fairly even:

> local_bag_all.py 0/ no-bag
...yields 1/11 of the total b numbers (because of the additional x checksum)
...processes mets only.

> local_bag_all.py 0/3/4/2 bag
...yields about 0.01% of all the b numbers

> local_bag_all.py 0/3/4 bag
...yields about 0.1% of all the b numbers

"""

import sys
import time
import logging
import bagger_processor
import aws
from mets_filesource import bnumber_generator


def main():
    if len(sys.argv) != 3:
        print("usage: local_bag_all.py <filter> <bag|no-bag>")
        print("e.g.,  local_bag_all.py x/1/2 no-bag")
    else:
        filter = sys.argv[1]
        skip = sys.argv[2] != "bag"
        start = time.time()
        counter = 0
        for b_number in bnumber_generator(filter):
            logging.debug("processing " + b_number)
            message = {"identifier": b_number, "do_not_bag": skip}
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
