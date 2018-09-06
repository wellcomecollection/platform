import sys
import time
from mets_filesource import b_numbers_from_fileshare, b_numbers_from_s3


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
    for b in b_numbers_from_s3(filter):
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
