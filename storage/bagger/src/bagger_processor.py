import time
import datetime

import logging
import traceback

import bagger
import notifier


def process_bagging_message(message):
    identifier = message.get("identifier", None)
    do_not_bag = message.get("do_not_bag", True)

    result = {
        "identifier": identifier,
        "upload_location": None,
        "duration": -1,
        "error": None,
        "created": datetime.datetime.now().isoformat(),
    }

    if identifier is not None:
        start = time.time()
        try:
            result["upload_location"] = bagger.bag_from_identifier(
                identifier, do_not_bag
            )
            result["duration"] = time.time() - start
            notifier.bagging_complete(result)
        except Exception:
            # catch any kind of error
            tb = traceback.format_exc()
            logging.warning(tb)
            result["error"] = tb
            result["duration"] = time.time() - start

    return result
