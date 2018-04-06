# -*- encoding: utf-8

class Decision(Exception):
    path = None

    def __init__(self, path):
        self.path == path
        super(Decision, self).__init__()


class SignificantFile(Decision):
    is_significant = True


class InsignificantFile(Decision):
    is_significant = False


class UnrecognisedFile(SignificantFile):
    pass
