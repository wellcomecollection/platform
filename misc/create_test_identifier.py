#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Creates an identifier in the same scheme as the ID minter.

Useful for creating test IDs.

ABSOLUTELY NOT FOR PRODUCTION USE.
"""

import random
import string

allowed_chars = [
    char
    for char in (string.ascii_lowercase + string.digits)
    if char not in '0oil1'
]

while True:
    x = ''.join(random.choice(allowed_chars) for _ in range(8))
    if not x.startswith(tuple(string.digits)):
        print(x)
        break
