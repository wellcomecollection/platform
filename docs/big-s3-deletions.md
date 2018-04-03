# Doing a big S3 deletion

These are some lessons we learned while trying to delete 60m files from
an S3 bucket:

*   Make sure you have versioning disabled *before* you start deleting things.
    Otherwise you're just adding delete markers to the bucket.

*   You may see this error when trying to delete a bucket which appears
    to be empty:

    ```console
    $ aws s3 ls --recursive --summarize s3://miro-images-sync/

    Total Objects: 0
       Total Size: 0

    $ aws s3 rb s3://miro-images-sync
    remove_bucket failed: s3://miro-images-sync An error occurred (BucketNotEmpty) when calling the DeleteBucket operation: The bucket you tried to delete is not empty. You must delete all versions in the bucket.
    ```

    This means the bucket has versioning enabled, and there are old versions
    of deleted objects in the bucket.

*   The auto-expiry policy for deleting objects might work, but you have to
    define it in Terraform -- or it might be rolled back before it applies!

*   S3 supports multiple concurrent deletes if you spread them around, and this
    is usually faster than deleting in serial.  For example, if you have ten
    folders:

        01/
        02/
        03/
        04/
        05/
        06/
        07/
        08/
        09/
        10/

    It will probably be faster to have ten processes, each of which is deleting
    a single prefix, than to work through all ten folders one-at-a-time.

    S3 can't do that for you, because it doesn't know the structure of your
    bucket -- you need to script this manually.
