# Bagger

Bagger is a set of tools for creating bagit bags suitable for submission to the new [storage service](https://github.com/wellcometrust/platform/tree/master/docs/rfcs/002-archival_storage). 

It creates them from existing Wellcome digitised content. It simulates what Goobi would have produced, had that content been through a digitisation workflow that used the new storage service. It does this by converting the existing METS files, collecting the files they refer to (further METS manifestations, ALTO files, and the digital asset files themselves) and creating the zipped bag ready for ingest. It doesn't deposit the created bag into the new storage service; instead it puts the zipped bag into a bucket, ready for some other process to do that.

## Executables

The Python code comprises several runnable utilities; these are also accompanied by shell scripts for easier local execution and mixing in to other activities.

# B Number Generation

Various tools require a set of b numbers to work on, supplied by a filter expression. This can take three forms:

* a single b number, e.g., b12345678
* a path to a file containing b numbers, one per line
* a filter expression that yields all b numbers under a particular path in the METS source bucket.  Examples:
  * 1/ - all b numbers that end with ..1 (about 9% of the total)
  * 2/2 - all b numbers that end with ..22 (about 0.9% of the total)
  * x/1/3 - all b numbers that end with ..31x
  The first character in the filter (and hence, last in the b number) can be 0-9 or x (the possible check digits). The others must be 0-9.

# Tools

The following tools should be given the appropriate environment variables for staging or production storage services.

## Bagging

`queue_for_bagging.sh <filter|bnumber|file> <bag|no-bag>`

Put bagging instruction messages onto the bagger queue defined in the environment variable BAGGING_QUEUE

## Ingesting

`migtool.sh ingest --delay n --filter <filter|bnumber|file>`

Ingest all the b numbers provided, with a delay of n seconds between each (0 is fine)

This creates ingest API operations, instructing the storage service to ingest bags (zips) from the bucket provided by DROP_BUCKET_NAME.

## Updating status table

`migtool.sh update_status --delay 0  --filter <filter|bnumber|file>`

A DynamoDB table (specified in DYNAMO_TABLE) holds the status of b numbers. Some processes write directly to it - the bagger records the start and end times of bagging operations. Other data in the table can only be updated by a process that checks various outputs to see what's there (e.g., the errors bucket). This needs to be run to fully update the table.

Two additional parameters are available:

`migtool.sh update_status --delay 0  --filter <filter|bnumber|file> --check-package --check-alto`

`--check-package` will ask the DDS if it has successfully read the item from the storage service and constructed a _package_. This means it is able to generate a IIIF Manifest for it.

`--check-alto` will ask the DDS if it can successfully parse the ALTO associated with the object (which may have many manifestations each with many ALTOs). This means the DDS will be able to provide a IIIF Search API endpoint for the item.

These last two arguments will increase the time the update process takes and are not necessary for checking the bagging and ingest processes.


## Useful ways of managing errors

_(Many of these are now superseded by the DDS Dashboard)_

The bagger records the Python stack trace of errors and saves them to the error bucket.

If you want to delete all the errors, e.g., before a new run, you can just empty the error bucket entirely.
The bagger deletes an error for a b number after a successful bag.

You can generate an error report:

`error_report.sh` 

This prints the one line error message from the end of the stack trace.
You can look at the full stack trace of an error:

`error_report.sh b12345678`

If you want to delete an individual error manually:

`error_report.sh delete b12345678`

Sometimes you want to delete all errors of a particular type. You can generate an error report piped to a text file, and then generate a bulk delete operation. For example:

```
error_report.sh > errors.txt
sort --key=1.12 < errors.txt > sorted_errors.txt
grep "Expected 1 AMD ref" sorted_errors.txt | sed -n -e 's/^\(b[0123456789x]*\).*/\. error_report.sh delete \1/p' > mass_delete.sh
```

And then run

```
mass_delete.sh
```



# 

