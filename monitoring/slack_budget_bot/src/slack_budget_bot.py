# -*- encoding: utf-8 -*-

import datetime as dt
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
        return (self.data['BudgetName']
            .replace('budget', '')
            .replace('Budget', '')
            .strip())

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


def build_slack_payload(budgets, image_url):
    """
    Builds the payload that is sent to the Slack webhook about
    our budget overspend.
    """
    details = '\n'.join([
        f'{b.name}: {b.forecasted_spend} > {b.budget_limit}'
        for b in budgets
    ])

    return {
        'username': 'aws-budgets',
        'icon_emoji': ':money_with_wings:',
        'attachments': [
            {
                'color': 'warning',
                'title': 'AWS is forecasting an overspend on our budgets!',
                'image_url': image_url,
                'text': details,
            },
        ]
    }


def draw_diagram(budgets):
    """Draws a quick diagram to illustrate the overspend budgets."""
    import matplotlib
    import matplotlib.pyplot as plt
    from matplotlib.lines import Line2D
    from matplotlib.patches import Rectangle
    from matplotlib.text import Annotation

    # Define some parameters.  The Slack image previews are ~360x150px, so
    # we need fonts and sizes that fit that well.
    matplotlib.rcParams.update({
        'font.family': 'Arial',
        'font.size': 20,
        'figure.figsize': (11, 4.6),
    })

    fig, axes = plt.subplots()

    # First we draw a box plot.  We don't actually use any of the graph
    # elements it creates, but it sets up some axes, labels and tick marks
    # so we don't have to do that manually.
    #
    # Based on https://matplotlib.org/examples/pylab_examples/boxplot_demo.html
    data = [
        [b.budget_limit, b.current_spend, b.forecasted_spend]
        for b in budgets
    ]
    labels = [b.name for b in budgets]

    axes.boxplot(
        data,
        labels=labels,
        vert=False,

        # This parameter ensures that the boxplot elements are drawn on
        # a low layer in the image.
        zorder=0.0
    )

    for i, budget in enumerate(budgets, start=1):

        # Now we immediately discard most of what we've just drawn!
        # We draw  over it with a white box at a higher z-layer, so we can
        # draw lines ourselves.
        min_value = min([
            budget.budget_limit, budget.current_spend, budget.forecasted_spend
        ])
        max_value = max([
            budget.budget_limit, budget.current_spend, budget.forecasted_spend
        ])

        axes.add_patch(
            Rectangle(
                xy=(min_value - 10, i - 0.75),
                width=(max_value - min_value + 10),
                height=0.5,
                fill=True,
                color='white',
                zorder=1.0
            )
        )

        # Then we draw our own lines to show the different parts of
        # this budget.
        line_limit = Line2D(
            xdata=[budget.budget_limit, budget.budget_limit],
            ydata=[i - 0.2, i + 0.2],
            color='green',
            linewidth=6,
            linestyle=':'
        )
        axes.add_line(line_limit)

        line_forecast = Line2D(
            xdata=[budget.forecasted_spend, budget.forecasted_spend],
            ydata=[i - 0.2, i + 0.2],
            color='red',
            linewidth=6,
            linestyle=':'
        )
        axes.add_line(line_forecast)

        line_current = Line2D(
            xdata=[budget.current_spend, budget.current_spend],
            ydata=[i - 0.25, i + 0.25],
            color='black',
            linewidth=10
        )
        axes.add_line(line_current)

    # Finally, we add these three lines to the legend.  There's probably a
    # neater way of doing these with line styles, but I don't care enough to
    # learn how to do it "properly".
    legend_limit = Line2D(
        xdata=[], ydata=[], color='green', linestyle=':', label='budget limit'
    )
    legend_forecast = Line2D(
        xdata=[], ydata=[], color='red', linestyle=':', label='forecast'
    )
    legend_current = Line2D(
        xdata=[], ydata=[], color='black', linestyle=':', label='current spend'
    )

    plt.legend(handles=[legend_limit, legend_forecast, legend_current])

    plt.savefig('figure.png', bbox_inches='tight')
    return 'figure.png'


def main(account_id, hook_url):
    all_budgets = get_budgets(account_id=account_id)

    overspend_budgets = [
        b
        for b in all_budgets
        if b.forecasted_spend > b.budget_limit
    ]

    if not overspend_budgets:
        print('No overspend in our budgets!  Nothing to do...')
        return

    filename = draw_diagram(overspend_budgets)

    s3_client = boto3.client('s3')
    s3_key = f'budget_graphs/{dt.datetime.now.isoformat()}'

    s3_client.upload_file(
        Filename=filename,
        Bucket=s3_bucket,
        Key=s3_key,
        Acl='public'
    )

    payload = build_slack_payload(
        overspend_budgets,
        image_url=f'https://s3-eu-west-1.amazonaws.com/{s3_bucket}/{s3_key}'
    )
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
