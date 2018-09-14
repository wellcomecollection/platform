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
            print("{0}: {1}".format(bnumber, message))


if __name__ == "__main__":
    main()
