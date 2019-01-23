"""Generates b numbers from the S3 keys in the METS BUCKET

This is useful for providing a stream of valid b numbers for testing.

USAGE:

> mets_queuer.py
Yield all b numbers! (~250,000)

> mets_queuer.py <filter>
Limit the b numbers returned to a filtered set, based on keys.
The spread of b numbers is fairly even:

> mets_queuer.py 0/
...yields 1/11 of the total b numbers (because of the additional x checksum)

> mets_queuer.py 0/3/4/2
...yields about 0.01% of all the b numbers

> mets_queuer.py 0/3/4
...yields about 0.1% of all the b numbers
"""

import sys
import time
from mets_filesource import b_numbers_from_fileshare, bnumber_generator


def main():
    if len(sys.argv) == 2 and sys.argv[1] == "filesystem":
        print_from_filesystem()
    else:
        filter = ""
        if len(sys.argv) > 1:
            filter = sys.argv[1]
        print_from_s3(filter)


def print_from_s3(filter):
    start = time.time()
    counter = 1
    for b in bnumber_generator(filter):
        print("{0: <6} | {1}".format(counter, b))
        counter = counter + 1
    end = time.time()
    time_taken = end - start
    print("retrieved {0} b numbers in {1} seconds".format(counter, time_taken))


def print_from_filesystem():
    root = sys.argv[2]
    counter = 1
    for b in b_numbers_from_fileshare(root):
        print("{0: <6} | {1}".format(counter, b))
        counter = counter + 1


if __name__ == "__main__":
    main()
