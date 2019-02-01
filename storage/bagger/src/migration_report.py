"""
The status table has the following fields:
registered:        timestamp - marker to establish presence of b num
updated:           the last time the updater code ran on this b number
bagger_batch_id:   batch used in conjunction with...
bagger_filter:     ...filter expression for that batch
bagger_start:
bagger_end:
bag_date:
bag_size:
mets_error:
ingest_batch_id:
ingest_filter:    ...filter expression for that batch
ingest_start:     NEW, use ingest date if missing
ingest_id:
ingest_date:
ingest_status:
package_date:
texts_expected:
texts_cached:
dlcs_mismatch:
"""
from decimal import Decimal
import time
import status_table
import aws


# rank by count, batch date
# include errors in object? yes, probably
# each row...


def make_report():

    batches = {}
    global_batch = get_batch(batches, "global", "global", None)

    start = time.time()
    counter = 0
    for item in status_table.all_items():
        counter = counter + 1
        bagger_batch_id = item.get("bagger_batch_id", None)
        if bagger_batch_id is not None:
            bagger_filter = item.get("bagger_filter")
            bagger_batch = get_batch(batches, bagger_batch_id, "bagger", bagger_filter)
            update_batch(bagger_batch, item)

        ingest_batch_id = item.get("ingest_batch_id", None)
        if ingest_batch_id is not None:
            ingest_filter = item.get("ingest_filter")
            ingest_batch = get_batch(batches, ingest_batch_id, "ingest", ingest_filter)
            update_batch(ingest_batch, item)

        update_batch(global_batch, item)

    # for b in batches.values():
    #     b["bagging_errors"] = len(b["bagging_errors"])
    print("scanned {0} items in {1} s".format(counter, time.time() - start))
    aws.save_migration_report(batches)


def get_batch(batches, id, batch_type, filter):
    batch = batches.get(id, None)
    if batch is None:
        batch = make_new_batch(id, batch_type, filter)
        batches[id] = batch
    return batch


def update_batch(batch, item):
    bnumber = item["bnumber"]
    requires_update = False
    batch_type = batch["type"]
    batch["count"] = batch["count"] + 1

    updated = item.get("updated", "0")
    batch["earliest_update"] = min(batch["earliest_update"], updated)
    batch["latest_update"] = max(batch["latest_update"], updated)

    batch_date = "0"
    if batch_type == "bagger":
        batch_date = item.get("bagger_start", "0")
    elif batch_type == "ingest":
        batch_date = item.get("ingest_start", None)
        if batch_date is None:
            # before deployment of ingest_start
            batch_date = item.get("ingest_date", "0")
    batch["earliest_batch_date"] = min(batch["earliest_batch_date"], batch_date)
    batch["latest_batch_date"] = max(batch["latest_batch_date"], batch_date)

    # bag status
    bagging_error = item.get("mets_error", None)
    bag_date = item.get("bag_date", "0")
    if bagging_error == "-":
        bagging_error = None
    if bagging_error is None:
        bagger_start = item.get("bagger_start", "0")
        bagger_end = item.get("bagger_end", "0")
        if not (bagger_start < bag_date and bagger_end > bag_date):
            requires_update = True

        if bagger_end > bagger_start:
            batch["bag_success_count"] = batch["bag_success_count"] + 1
        else:
            batch["bag_fail_count"] = batch["bag_fail_count"] + 1
    else:
        batch["bagging_errors"].append({bnumber: bagging_error})

    # ingest status
    ingest_status = item.get("ingest_status", None)
    if ingest_status == "-" or ingest_status == "no-ingest":
        ingest_status = None

    ingest_start = item.get("ingest_start", None)
    if ingest_start is None:
        # before deployment of ingest_start
        ingest_start = item.get("ingest_date", "0")

    if ingest_status == "succeeded" and bag_date > "0" and ingest_start > bag_date:
        batch["ingest_success_count"] = batch["ingest_success_count"] + 1
    else:
        if ingest_status is not None and ingest_status != "succeeded":
            batch["ingest_unsuccessful"].append({bnumber: ingest_status})
        else:
            batch["ingest_fail_count"] = batch["ingest_fail_count"] + 1

    # package status
    package_date = item.get("package_date", "0")
    package_valid = False
    if package_date > ingest_start and ingest_status == "succeeded":
        batch["packaged"] = batch["packaged"] + 1
        package_valid = True
    else:
        batch["not_packaged"] = batch["not_packaged"] + 1

    # text status
    texts_expected = item.get("texts_expected", -1)
    texts_cached = item.get("texts_cached", 0)
    if texts_expected == texts_cached:
        batch["text_expected"] = batch["text_expected"] + 1
    else:
        if texts_expected == -1:
            batch["text_unknown"] = batch["text_unknown"] + 1
        else:
            batch["text_unexpected"] = batch["text_unexpected"] + 1

    # DLCS sync

    dlcs_mismatch = item.get("dlcs_mismatch", -1)
    if dlcs_mismatch == 0 and package_valid:
        batch["dlcs_sync_ok"] = batch["dlcs_sync_ok"] + 1
    else:
        if dlcs_mismatch == -1:
            batch["dlcs_sync_unknown"] = batch["dlcs_sync_unknown"] + 1
        else:
            batch["dlcs_sync_problem"] = batch["dlcs_sync_problem"] + 1

    if requires_update:
        batch["require_update"] = batch["require_update"] + 1

    # Use this to step through batch by batch
    # print("{0}: {1}".format(batch["id"], batch["count"]))
    # print("------------------------")
    # print(json.dumps(item, indent=4, default=extra_encoding))
    # print(json.dumps(batch, indent=4))
    # input("continue...")


def make_new_batch(id, batch_type, filter):
    return {
        "id": id,  # bagger_batch_id or ingest_batch_id or "global"
        "type": batch_type,  # bagger or ingest or global
        "filter": filter,
        "count": 0,  # number of rows in batch
        "require_update": 0,  # items where we can deduce an update has not run
        "earliest_update": "0",  # min(updated)
        "latest_update": "0",  # max(updated)
        "earliest_batch_date": "0",  # min(bagger_start or ingest_date)
        "latest_batch_date": "0",  # max(bagger_start or ingest_date)
        # pie for bags  .. these three should add up to count (check!)
        "bag_success_count": 0,  # bags started, ended after start, and no error
        "bag_fail_count": 0,  # bags started but not finished, and no error
        "bagging_errors": [],  # list of { bnum: error_str } len(..) => err count (only when err str)
        # pie for ingested - ingest_start may not be present
        "ingest_success_count": 0,  # ingest "succeeded" and ingest_date >= (ingest_start or ingest_date AND ingest_start)
        "ingest_fail_count": 0,  # latest ingest not equal to "succeeded"
        "ingest_unsuccessful": [],  # ? do we need this one?
        # pie for package # TODO - per manifestation checks like ALTO? Should add up to count
        "packaged": 0,  # package date exists and is > succeeded ingest date (which also must exist)
        "not_packaged": 0,  # anything else, not 3-state
        # pie for text 3-state
        "text_expected": 0,  # count of texts_expected == texts_cached  (-1 or null?)
        "text_unexpected": 0,  # count of ^ present but !=
        "text_unknown": 0,  # count of null texts_expected
        "dlcs_sync_ok": 0,  # number of items with dcls_mismatch == 0
        "dlcs_sync_problem": 0,  # number of items with dcls_mismatch > 0
        "dlcs_sync_unknown": 0,  # no syncinfo
    }


def extra_encoding(o):
    if isinstance(o, Decimal):
        return int(o)
