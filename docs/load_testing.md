# Load Testing guidelines

Here are some guidelines established while trying to load test our platform.

## Define your requirements up front

It's a good idea to start with a clear idea of where you want to get to. Define how many requests per second consitute a passing load test and lay those assertions down in code.

## Don't load test your load tester

Where you run your load test from has an impact on the results of your test. If you're trying to receive a lot of large responses back from your target under test you could well bottleneck on the load tester end.

## Soak testing

Short load tests are useful but don't give you an idea of what will happen under sustained load. Is there a memory leak in your app? Will you eventually fill the disk with logs? The best way to test is run a "soak" test, i.e. run your application under load for a few days at least to find out what will happen.

## Continuous load testing

Any code or infrastructure change (including network outside of your control) could result in a change in application performance not necessarily visibly under normal traffic. In order to catch issues as soon as possible it's a good idea to regularly run load tests against your project and report the results back somewhere visible.

## Record everything

You're going to be running a lot of load tests, it's a good idea to record everything so it can be referenced later - along with the conditions of the test to understand how the test results vary under different conditions.

## Alert on failure

So you're running tests regularly and you have a set of clear requirements to test the results of those tests again. Set up an alert of some kind to notify you if the results dip beneath your requirments.
