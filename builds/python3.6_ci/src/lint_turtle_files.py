#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
This script takes the path to a directory, and looks for any Turtle files
(https://www.w3.org/TeamSubmission/turtle/), then uses RDFLib to check if
they're valid TTL.

It exits with code 0 if all files are valid, 1 if not.
"""

print("Hello, I am turtle linter")
