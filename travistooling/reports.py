# -*- encoding: utf-8


def build_report_output(report, description='run tests'):
    """
    Build a human-readable string that explains why we are/aren't running tests.
    """
    lines = []

    for significance, prefix in [
        (True, 'Reasons to'),
        (False, 'Reasons not to'),
    ]:
        if report[significance]:
            lines.append('## %s %s ##' % (prefix, description))
            for reason, affected_paths in report[significance].items():
                lines.append('\n%s:' % reason.__name__)
                for p in sorted(affected_paths):
                    lines.append(' - %s' % p)

            lines.append('\n')

    return '\n'.join(lines).strip()
