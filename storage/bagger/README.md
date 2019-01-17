# Bagger

Bagger is a set of tools for creating bagit bags suitable for submission to the new [storage service](https://github.com/wellcometrust/platform/tree/master/docs/rfcs/002-archival_storage). 

It creates them from existing Wellcome digitised content. It simulates what Goobi would have produced, had that content been through a digitisation workflow that used the new storage service. It does this by converting the existing METS files, collecting the files they refer to (further METS manifestations, ALTO files, and the digital asset files themselves) and creating the zipped bag ready for ingest. It doesn't deposit the created bag into the new storage service; instead it puts the zipped bag into a bucket, ready for some other process to do that.

## Executables

The Python code comprises several runnable utilities; these are also accompanied by shell scripts for easier local execution and mixing in to other activities.

## S3 buckets

The various tools read from and write to S3 buckets. The actual bucket names are provided to the running code via environment variables used in `settings.py`. The following descriptions use the name the bucket:

* METS_BUCKET_NAME: contains a copy of the METS files on disk at Wellcome. This is synchronised manually. As well as a full duplicate under the key prefix `mets/`, the bucket also contains a copy without METS-ALTO files, under the 

# B Number Genera




## Useful tricks for managing errors

If you want to delete all the errors, e.g., before a new run, you can just empty the error bucket entirely.

If you want to delete an individual error

Sometimes you want to delete all errors of a pa

Deleting all errors of a certain type

grep "Expected 1 AMD ref" sorted_errors.txt | sed -n -e 's/^\(b[0123456789x]*\).*/\. error_report.sh delete \1/p' > mass_delete.sh

sort --key=1.12 < errors.txt > sorted_errors.txt