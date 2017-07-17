# tif-conversion

This is the script I used to bulk-convert a series of JPEG2000 images into TIF images.

A brief outline of the process:

*   We have a collection of JP2 images in an S3 bucket (`SRC_BUCKET`)

*   We want to convert them into TIF images, and store them in a second S3 bucket (`DST_BUCKET`)

*   A Python script (not included here) created an SQS entry for every image in the source bucket, of the form:

    ```json
    {
      "key": "images/C0000000/C0000001.jp2"
    }
    ```

    One entry per original JP2.

*   The script in this directory was bundled in a Dockerfile, and run concurrently many times using ECS (about 100 instances at peak).
    It reads an SQS entry, extracts the key, fetches the image from the source bucket, converts to TIF and pushes the new image to the second bucket.
