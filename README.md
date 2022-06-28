
# personal-income-tax-submission-fronted
This is where users can review and make changes to the dividends, Interest and gift aid sections of their income tax return.

## Running the service locally WITH BEANS

You will need to have the following:
- Installed [MongoDB](https://docs.mongodb.com/manual/installation/)
- Installed/configured [service manager](https://github.com/hmrc/service-manager).

The service manager profile for this service is:

    sm --start PERSONAL_INCOME_TAX_SUBMISSION_FRONTEND
Run the following command to start the remaining services locally:

    sudo mongod (If not already running)
    sm --start INCOME_TAX_SUBMISSION_ALL -r

This service runs on port: `localhost:9308`

### Feature Switches

| Feature | Environments Enabled In |
| --- | --- |
| Encryption | QA, Staging, Production |
| Dividends | Local, QA, Staging, Production |
| Interest | Local, QA, Staging, Production |
| GiftAid | Local, QA, Staging, Production |
| NRS | Local |
| Tax Year Error | Production |
| Welsh | Local, QA, Staging |
| Tailoring | Local |

## How to access this service
Each of the journeys contained in this service can be access through [income-tax-submission-frontend](https://github.com/hmrc/income-tax-submission-frontend).


# Journeys

Each journey allows a user to submit new data or append previously submitted data.

## Dividends

<details>
<summary>Click here to see an example of a user with previously submitted dividend data(JSON)</summary>

```json
{
  "dividends": {
    "ukDividends": 99999999999.99,
    "otherUkDividends": 99999999999.99
  }
}
```
</details>

<details>
<summary>Click here to see an example of a dividend submission(JSON)</summary>

```json
{
  "ukDividends": true,
  "ukDividendsAmount": 500,
  "otherUkDividends": true,
  "otherUkDividendsAmount": 500
}
```
</details>

## Interests

<details>
<summary>Click here to see an example of a user with previously submitted interest data(JSON)</summary>

```json
{
  "interest": [
    {
      "accountName": "Rick Owens Bank",
      "incomeSourceId": "000000000000001",
      "taxedUkInterest": 99999999999.99,
      "untaxedUkInterest": 99999999999.99
    },
    {
      "accountName": "Rick Owens Taxed Bank",
      "incomeSourceId": "000000000000002",
      "taxedUkInterest": 99999999999.99
    },
    {
      "accountName": "Rick Owens Untaxed Bank",
      "incomeSourceId": "000000000000003",
      "untaxedUkInterest": 99999999999.99
    }
  ]
}
```
</details>

<details>
<summary>Click here to see an example of a interest submission(JSON)</summary>

```json
{
  "untaxedUkInterest": true,
  "taxedUkInterest": true,
  "accounts": [
    {
      "accountName": "juamal",
      "untaxedAmount": 566,
      "taxedAmount": 500,
      "uniqueSessionId": "c861a963-e126-402a-9909-da37d9f77121"
    }
  ]
}
```
</details>

## GiftAid

<details>
<summary>Click here to see an example of a user with previously submitted gift aid data(JSON)</summary>

```json
{
  "giftAid": {
    "giftAidPayments": {
      "nonUkCharitiesCharityNames": [
        "Rick Owens Charity"
      ],
      "currentYear": 99999999999.99,
      "oneOffCurrentYear": 99999999999.99,
      "currentYearTreatedAsPreviousYear": 99999999999.99,
      "nextYearTreatedAsCurrentYear": 99999999999.99,
      "nonUkCharities": 99999999999.99
    },
    "gifts": {
      "investmentsNonUkCharitiesCharityNames": [
        "Rick Owens Non-UK Charity"
      ],
      "landAndBuildings": 99999999999.99,
      "sharesOrSecurities": 99999999999.99,
      "investmentsNonUkCharities": 99999999999.99
    }
  }
}
```
</details>

<details>
<summary>Click here to see an example of a gift aid submission(JSON)</summary>

```json
{
  "giftAidPayments": {
    "nonUkCharities": 500,
    "nonUkCharitiesCharityNames": [
      "charity"
    ],
    "currentYear": 500,
    "currentYearTreatedAsPreviousYear": 500,
    "nextYearTreatedAsCurrentYear": 500,
    "oneOffCurrentYear": 500
  },
  "gifts": {
    "investmentsNonUkCharities": 500,
    "investmentsNonUkCharitiesCharityNames": [
      "charity"
    ],
    "sharesOrSecurities": 500,
    "landAndBuildings": 500
  }
}
```
</details>

## Nino containing previous data
| Nino | Income sources data |
| --- | --- |
| AA123459A | User with data for dividends, interest and gift-aid income sources |
| AA000001A | User with dividends data |
| AA000003A | User with dividends and interest data with multiple accounts|
| AA000002A | User with interest data end of year with multiple accounts|
| AA637489D | User with gift-aid data |


### License

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
