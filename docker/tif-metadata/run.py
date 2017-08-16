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

import json
import subprocess
import os
import tempfile

import boto3
import docopt


def start_download_process(task):
    _, tmp_fp = tempfile.mkstemp()
    s3_source = build_s3_source(task)
    p = subprocess.Popen(["aws", "s3", "cp", s3_source, tmp_fp])
    return p, task, tmp_fp


def start_upload_process(local_image_path, task):
    s3_location = build_s3_destination(task)
    print(s3_location)
    p = subprocess.Popen(["aws", "s3", "cp", local_image_path, s3_location])
    return p, local_image_path, s3_location


def build_s3_destination(task):
    s3_destination_bucket = task['destination']['bucket_name']
    s3_destination_path = task['destination']['object_path']
    s3_location = f"s3://{s3_destination_bucket}/{s3_destination_path}"
    return s3_location


def build_s3_source(task):
    s3_source_bucket = task["source"]["bucket_name"]
    s3_source_path = task["source"]["object_path"]
    s3_source = f"s3://{s3_source_bucket}/{s3_source_path}"
    return s3_source


def wait(process, success_message, failure_message):
    process.wait()
    if process.returncode != 0:
        raise Exception(failure_message)
    else:
        print(success_message)


def embed_image_metadata(task, local_image_path):
    print(task)
    xmp_metadata = task["metadata"]["xmp"]
    arguments = [f"-xmp:{key}=\"{value}\"" for key, value in xmp_metadata.items() if value is not None]
    arguments_line = " ".join(arguments)
    print(f"exiftool -m -sep \", \" {arguments_line} {local_image_path}")


def main():
    args = docopt.docopt(__doc__)

    job_bucket = args["--bucket-name"]
    job_s3_key = args["--key"]
    delete_original = bool(args['--delete-original'])

    client = boto3.client("s3")
    job_filename = os.path.basename(job_s3_key)
    client.download_file(Bucket=job_bucket,
                         Key=job_s3_key,
                         Filename=job_filename)

    with open(job_filename, "r") as job_file:
        job = json.load(job_file)

    tasks = job['task_list']
    print(f"Starting download of {len(tasks)} images from s3")

    processes = [start_download_process(task) for task in tasks]

    for process, task, _ in processes:
        s3_source = build_s3_source(task)
        success_message = f"Downloading of {s3_source} succeeded"
        failure_message = f"Downloading of {s3_source} failed!"
        wait(process, success_message, failure_message)

    print("Finished downloading s3 files!")

    for _, task, local_image_path in processes:
        embed_image_metadata(task, local_image_path)

    print(f"Starting upload of {len(tasks)} images from s3")
    upload_processes = [
        start_upload_process(local_image_path, task)
        for _, task, local_image_path in processes
    ]

    try:
        for process, _, upload_location in upload_processes:
            failure_message = f"Uploading to {upload_location} failed!"
            success_message = f"Uploading to {upload_location} succeeded"
            wait(process,
                 success_message,
                 failure_message)
            # TODO delete original file if --delete-original flag is set
    finally:
        for _, local_image_path, _ in upload_processes:
            os.unlink(local_image_path)

    print("Finished uploading files to s3!")


if __name__ == '__main__':
    main()
