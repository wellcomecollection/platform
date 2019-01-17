def normalise_b_number(b_str):
    b_str = str(b_str)
    if b_str[0] == "b":
        b_str = b_str[1:]

    # TODO - analyse digits, checksum etc
    # temporary -

    if len(b_str) != 8:
        raise ValueError("Not a valid b number")

    # if b_str == "19974760":
    #     raise ValueError("Will not process Chemist and Druggist right now")

    return "b" + b_str
