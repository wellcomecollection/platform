import sys
import time
import logging
import aws
from mets_filesource import b_numbers_from_s3


def main():
    if len(sys.argv) != 3:
        print("usage: local_enqueue.py <filter|bnumber> <bag|no-bag>")
        print("e.g.,  local_enqueue.py x/1/2 no-bag")
        print("e.g.,  local_enqueue.py b12345678 bag")
    else:
        to_process = sys.argv[1]
        skip = sys.argv[2] != "bag"
        info = "for bagging"
        if(skip):
            info = "METS only"
        start = time.time()
        counter = 0
        if to_process.startswith("b"):
            generator = (b for b in [to_process])
        else:
            generator = b_numbers_from_s3(to_process)

        for b_number in generator:
            counter = counter + 1
            logging.info("processing " + b_number)
            message = {
                "identifier": b_number,
                "do_not_bag": skip
            }
            print("{0}: enqueueing {1}, {2}".format(counter, b_number, info))
            response = aws.send_bag_instruction(message)
            message["MessageId"] = response.get("MessageId", "NO-MESSAGE-ID")
            print(message)
            print("-------------------")

        time_taken = time.time() - start
        print("----------------")
        print("{0} items in {1} seconds.".format(counter, time_taken))


if __name__ == "__main__":
    main()
