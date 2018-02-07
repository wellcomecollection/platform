# -*- encoding: utf-8 -*-

import json
import os

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

    def __str__(self):
        if self.unit == 'USD':
            return f'${self.amount:.2f}'
        else:
            return f'{self.unit} {self.amount:.2f}'

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
    def name(self):
        return self.data['BudgetName']

    @property
    def budget_limit(self):
        spend = self.data['BudgetLimit']
        return CurrencyAmount(unit=spend['Unit'], amount=spend['Amount'])

    @property
    def current_spend(self):
        spend = self.data['CalculatedSpend']['ActualSpend']
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


def build_slack_payload(budget):
    """
    Builds the payload that is sent to the Slack webhook about
    our budget overspend.
    """
    return {
        'username': 'aws-budgets',
        'icon_emoji': ':money_with_wings:',
        'attachments': [
            {
                'color': 'warning',
                'title': f'{budget.name} is forecast for an overspend!',
                'fields': [{
                    'value': '\n'.join([
                        f'Budget:   {budget.budget_limit}',
                        f'Current:  {budget.current_spend}',
                        f'Forecast: {budget.forecasted_spend}',
                    ])
                }]
            }
        ]
    }


def main(account_id, hook_url):
    budgets = get_budgets(account_id=account_id)

    for b in budgets:
        if b.budget_limit < b.forecasted_spend:
            payload = build_slack_payload(budget=b)
            resp = requests.post(
                hook_url,
                data=json.dumps(payload),
                headers={'Content-Type': 'application/json'}
            )
            resp.raise_for_status()


if __name__ == '__main__':

    account_id = os.environ['ACCOUNT_ID']
    hook_url = os.environ['SLACK_WEBHOOK']

    main(account_id=account_id, hook_url=hook_url)
