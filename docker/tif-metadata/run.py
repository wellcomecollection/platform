#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Usage:
  run.py --bucket-name=<BUCKET_NAME> --key=<KEY> [--delete-original]
  run.py -h | --help

Options:
  -h --help              Show this message.
  --bucket-name=<BUCKET_NAME>
  --key=<>
  --delete-original      Delete the original source key.

"""

import subprocess
import tempfile
import hashlib

import json
import boto3
import docopt
from os.path import basename


args = docopt.docopt(__doc__)

job_bucket = args["--bucket-name"]
job_s3_key = args["--key"]
delete_original = bool(args['--delete-original'])
# actually this should be in the json job separate from the key
image_bucket = "wellcomecollection-tif-derivatives"

client = boto3.client("s3")
job_filename = basename(job_s3_key)
client.download_file(Bucket=job_bucket, Key=job_s3_key,
                     Filename=job_filename)

with open(job_filename, "r") as job_file:
    job = json.load(job_file)

tasks = job['task_list']
print(f"Starting download of {len(tasks)} images from s3")
processes = []
for task in tasks:
    _, tmp_fp = tempfile.mkstemp()
    s3_source = task["source"]
    p = subprocess.Popen(["aws", "s3", "cp", s3_source, tmp_fp])
    processes.append((p, task, tmp_fp))

for process, task, _ in processes:
    process.wait()
    s3_source = task["source"]
    if process.returncode != 0:
        raise Exception(f"Downloading of {s3_source} failed!")
    else:
        print(f"Downloading of {s3_source} succeeded")

print("Finished downloading s3 files!")

for _, task, local_image_path in processes:
    print(task)
    location = task['shoot']["location"] or None
    creator = task['shoot']["staff_photog"] or task['shoot']["freelance_photog"] # or freelance_photog?? Check one then the other? Can they both be set?
    caption = task['image']["caption"] or None
    intended_usage = task['shoot']["intended_usage"] or None
    copyright = task['image']["credit_line"] or None
    print(f"location: {location}")
    print(f"creator: {creator}")
    print(f"creator: {creator}")
    print(f"caption: {caption}")
    print(f"intended_usage: {intended_usage}")
    print(f"copyright: {copyright}")
    # missing subject, usage terms, licence
    # Magic happens here...
    # exiftool -m -sep ", " -xmp:Location="$Location" -xmp:Creator="$Photog" -xmp:Description="$Caption" -xmp:Subject="$Keywords" -xmp:Instructions="$IntendedUsage" -xmp:UsageTerms="$UsageTerms" -xmp:Copyright="Wellcome" -xmp:License="$CC_URL" $Filename

print(f"Starting upload of {len(tasks)} images from s3")
upload_processes = []
for _, task, local_image_path in processes:
    reference = task["shoot"]["reference"]
    filename = basename(task["source"])
    # copied logic from platform private :( Maybe should be part of the tak in the json?
    h = hashlib.md5()
    h.update(reference.encode('utf8'))
    shoot_hash=h.hexdigest()[:2]
    s3_location = f"s3://{image_bucket}/shoots/{shoot_hash}/{reference}/{filename}"
    p = subprocess.Popen(["aws", "s3", "cp", local_image_path, s3_location])
    upload_processes.append((p,s3_location))

for (process, upload_location) in upload_processes:
    process.wait()
    if process.returncode != 0:
        raise Exception(f"Uploading to {upload_location} failed!")
    else:
        print(f"Uploading to {upload_location} succeeded")

print("Finished uploading files to s3!")