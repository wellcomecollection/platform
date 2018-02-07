# -*- encoding: utf-8 -*-

import attr
import boto3
import requests


@attr.s
class CurrencyAmount:
    unit = attr.ib()

    # Turning strings like '233.205000000000012505552149377763271331787109375'
    # into floats is a little cheaty, but a floating-point error here is
    # insignificant, so we'll take the risk!
    amount = attr.ib(converter=float)

    def __gt__(self, other):
        if self.unit != other.unit:
            raise ValueError(
                "Cannot compare {type(self).__name__} with different "
                "currencies: {self.unit!r} != {other.unit!r}"
            )
        return self.amount > other.amount

    def __lt__(self, other):
        if self.unit != other.unit:
            raise ValueError(
                "Cannot compare {type(self).__name__} with different "
                "currencies: {self.unit!r} != {other.unit!r}"
            )
        return self.amount < other.amount


@attr.s
class Budget:
    """Wrapper around an budget from the DescribeBudgets API."""
    data = attr.ib()

    @property
    def budget_limit(self):
        spend = self.data['BudgetLimit']
        return CurrencyAmount(unit=spend['Unit'], amount=spend['Amount'])

    @property
    def forecasted_spend(self):
        spend = self.data['CalculatedSpend']['ForecastedSpend']
        return CurrencyAmount(unit=spend['Unit'], amount=spend['Amount'])


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

    bad_budgets = [b for b in budgets if b.budget_limit < b.forecasted_spend]

    from pprint import pprint
    pprint(bad_budgets)


if __name__ == '__main__':
    main()
