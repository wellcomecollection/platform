# -*- encoding: utf-8 -*-

import attr
import boto3


@attr.s
class Budget(object):
    """Wrapper around an budget from the DescribeBudgets API."""
    data = attr.ib()

    @property
    def actual_spend(self):
        return float(self.data['CalculatedSpend']['ActualSpend']['Amount'])

    @property
    def forecasted_spend(self):
        return float(self.data['CalculatedSpend']['ForecastedSpend']['Amount'])


def get_budgets(account_id):
    """
    Returns all the budgets for a given account.
    """
    client = boto3.client('budgets')

    # Describe the current budgets on the account
    resp = client.describe_budgets(AccountId=account_id)

    for b in resp['Budgets']:
        yield Budget(b)


def main():
    budgets = get_budgets(account_id='760097843905')


if __name__ == '__main__':
    main()
