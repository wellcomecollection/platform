#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Usage:
  run.py --bucket-name=<BUCKET_NAME> --key=<KEY> [--no-fail-on-error]
  run.py -h | --help

Options:
  -h --help                     Show this message.
  --bucket-name=<BUCKET_NAME>   Name of the bucket where the job json file resides.
  --key=<KEY>                   Key of the job json file.
  --no-fail-on-error            Do not exit with on-zero exit code if metadata application fails

"""

import json
import subprocess
import os
import tempfile

import boto3
import docopt


def download_in_parallel(tasks):
    print(f"Starting download of {len(tasks)} images from s3")
    processes = [start_download_process(task) for task in tasks]
    images = [check_status(local_image_path, process, task) for process, task, local_image_path in processes]
    return images


def upload_in_parallel(images):
    print(f"Starting upload of {len(images)} images to s3")
    upload_processes = [
        start_upload_process(local_image_path, task)
        for task, local_image_path in images
    ]
    for process, local_image_path, upload_location in upload_processes:
        failure_message = f"Uploading from {local_image_path} to {upload_location} failed!"
        success_message = f"Uploading from {local_image_path} to {upload_location} succeeded"
        wait(process,
             success_message,
             failure_message)
        # TODO delete original file if --delete-original flag is set


def check_status(local_image_path, process, task):
    s3_source = build_s3_source(task)
    success_message = f"Downloading of {s3_source} to {local_image_path} succeeded"
    failure_message = f"Downloading of {s3_source} to {local_image_path} failed!"
    wait(process, success_message, failure_message)
    return task, local_image_path


def start_download_process(task):
    _, tmp_fp = tempfile.mkstemp()
    s3_source = build_s3_source(task)
    p = subprocess.Popen(["aws", "s3", "cp", s3_source, tmp_fp])
    return p, task, tmp_fp


def start_upload_process(local_image_path, task):
    s3_location = build_s3_destination(task)
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
    if process.wait() != 0:
        raise Exception(failure_message)
    else:
        print(success_message)


def embed_image_metadata(task, local_image_path, no_fail_on_error, debug=False):
    xmp_metadata = task["metadata"]["xmp"]

    arguments = [build_exiftool_argument(key, value) for key, value in xmp_metadata.items() if value is not None]
    sep_arguments = ["exiftool", "-m", "-sep", ","] + arguments
    sep_arguments.append(local_image_path)

    if debug:
        print('Running:')
        print(' '.join(sep_arguments))
        exit(0)

    if subprocess.call(sep_arguments) != 0:
        if no_fail_on_error:
            task_id = task['id']
            outfile_name = f'/failed_tasks/{task_id}.json'

            print(f'Writing failed task {task} to {outfile_name}')

            with open(outfile_name, 'w') as outfile:
                json.dump(task, outfile)
        else:
            raise Exception("Failed embedding metadata!")
    else:
        print(f"Metadata embedded successfully for {build_s3_source(task)}")


def build_exiftool_argument(key, value):
    if isinstance(value, list):
        value = ",".join(value)
    return f"-xmp:{key}={value}"


def split(parallelism, tasks):
    return [tasks[i:i + parallelism] for i in range(0, len(tasks), parallelism)]


def main():
    args = docopt.docopt(__doc__)

    job_bucket = args["--bucket-name"]
    job_s3_key = args["--key"]
    no_fail_on_error = bool(args['--no-fail-on-error'])
    parallelism = 10

    print(f"Downloading {job_s3_key}")
    client = boto3.client("s3")

    job_filename = os.path.basename(job_s3_key)
    client.download_file(Bucket=job_bucket,
                         Key=job_s3_key,
                         Filename=job_filename)

    with open(job_filename, "r") as job_file:
        job = json.load(job_file)

    images = []
    try:
        for split_tasks in split(parallelism, job['task_list']):
            images += download_in_parallel(split_tasks)

        print("Finished downloading s3 files!")

        for task, local_image_path in images:
            embed_image_metadata(task, local_image_path, no_fail_on_error)

        for split_images in split(parallelism, images):
            upload_in_parallel(split_images)
    finally:
        for _, local_image_path in images:
            os.unlink(local_image_path)

    print("Finished uploading files to s3!")


if __name__ == '__main__':
    main()
