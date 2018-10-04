import logging
import traceback
import bagger
import time


def process_bagging_message(message):
    identifier = message.get("identifier", None)
    do_not_bag = message.get("do_not_bag", True)
    result = {"identifier": identifier, "duration": -1, "error": None}
    if identifier is not None:
        start = time.time()
        try:
            bagger.bag_from_identifier(identifier, do_not_bag)
        except Exception:
            # catch any kind of error
            tb = traceback.format_exc()
            logging.warn(tb)
            result["error"] = tb

        end = time.time()
        result["duration"] = end - start
    return result
