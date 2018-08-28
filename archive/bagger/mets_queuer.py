import sys
from mets_filesource import b_numbers_from_fileshare


def main():
    root = sys.argv[1]
    counter = 1
    for b in b_numbers_from_fileshare(root):
        print("{0: <6} | {1}".format(counter, b))
        counter = counter + 1


if __name__ == "__main__":
    main()


