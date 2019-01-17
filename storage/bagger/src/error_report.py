"""Fetch METS processing errors from the special error bucket.

As the bagger processes METS, it logs the stacktrace of any error as a JSON blob
in a special error bucket. This utility make it easier to see those errors, and
delete errors once the problem is fixed.

USAGE

> error_report.py
List all available errors with a single line summary

> error_report.py b12345678
View the full stack trace for a specific b number

> error_report.py delete b12345678
Remove the S3 object for this specific error

"""

import sys
import json
import aws


def main():
    bnumber = None
    if len(sys.argv) == 3 and sys.argv[1] == "delete":
        bnumber = sys.argv[2]
        print("attempt to delete " + bnumber)
        aws.remove_error(bnumber)
        return

    if len(sys.argv) == 2:
        bnumber = sys.argv[1]
        try:
            error = aws.get_error_for_b_number(bnumber)
            print(bnumber + ":")
            print(json.dumps(error, indent=4))
        except Exception as e:
            print("Could not find error for " + bnumber)
            print(e)
    else:
        for error in aws.get_all_errors():
            message = error["error"].splitlines()[-1]
            bnumber = error["identifier"]
            last_modified = error["last_modified"]
            print("{0}: {1} - {2}".format(bnumber, message, last_modified))


if __name__ == "__main__":
    main()
