# Simple Guide to Financial Concepts for European Option Pricing

## 1. Goal of the calculation

The goal of this module is to calculate the **theoretical price of a European option** using the **Black-Scholes model**.

Besides the option price, the module also calculates a group of measures called **Greeks**, which help explain how the option price changes when market variables change, such as the asset price, volatility, time, or interest rates.

The main idea is:

> Given a basic set of financial inputs, estimate how much a European option should be worth and how sensitive it is to market changes.

---

## 2. What is a financial option?

A financial option is a contract that gives someone the **right, but not the obligation**, to buy or sell an asset in the future at a fixed price.

That asset can be a stock, an index, a currency, or another financial instrument.

The important part is that the option holder is not forced to exercise the option. They will only use it if it is beneficial.

---

## 3. What is a Call option?

A **Call** is an option that gives the right to **buy** an asset at a fixed price in the future.

Simple example:

* A stock is worth 100 today.
* You have a Call option that lets you buy it at 110 in the future.

This option becomes more valuable if the stock price goes up.

For example, if the stock price rises to 130, being able to buy it at 110 is valuable.

In simple terms:

> A Call benefits when the asset price goes up.

---

## 4. What is a Put option?

A **Put** is an option that gives the right to **sell** an asset at a fixed price in the future.

Simple example:

* A stock is worth 100 today.
* You have a Put option that lets you sell it at 90 in the future.

This option becomes more valuable if the stock price goes down.

For example, if the stock price falls to 60, being able to sell it at 90 is valuable.

In simple terms:

> A Put benefits when the asset price goes down.

---

## 5. What does it mean for an option to be European?

A **European option** can only be exercised at its expiration date.

This does not mean the option is only used in Europe. It is just a classification of the option contract.

Another common type is the American option, which can be exercised at any time before expiration.

This module focuses on European options because they are simpler to model and are directly compatible with the classic Black-Scholes model.

---

## 6. What is the Spot price?

The **Spot price** is the current market price of the underlying asset.

Example:

* If a stock is worth 100 today, then the spot price is 100.

This value is the starting point of the calculation.

In simple terms:

> Spot = current price of the asset.

---

## 7. What is the Strike price?

The **Strike price** is the fixed price agreed in the option contract.

It is the price at which the asset can be bought or sold in the future, depending on whether the option is a Call or a Put.

Example for a Call:

* Current spot price: 100.
* Strike price: 110.

The option gives the right to buy the asset at 110 in the future.

Example for a Put:

* Current spot price: 100.
* Strike price: 90.

The option gives the right to sell the asset at 90 in the future.

In simple terms:

> Strike = fixed future buy or sell price.

---

## 8. What is Maturity?

**Maturity** is the time remaining until the option expires.

It is usually expressed in years.

Examples:

* 1 year = 1.0
* 6 months = 0.5
* 3 months = 0.25
* 30 days ≈ 30 / 365

In simple terms:

> Maturity = time left until expiration.

For the Black-Scholes endpoint in this first version, maturity must be greater than zero.
The exact expiration case (`maturity = 0`) is out of scope for this endpoint because the price becomes intrinsic payoff and some Greeks can be discontinuous or undefined.

The more time an option has until expiration, the more valuable it can be, because there is more time for the market to move in a favorable direction.

---

## 9. What is the risk-free rate?

The **risk-free rate** is a theoretical rate of return considered to have no risk.

In practice, it is often approximated using government bonds or similar instruments.

Examples:

* 5% per year is represented as 0.05.
* 10% per year is represented as 0.10.
* 2% per year is represented as 0.02.

In Black-Scholes, this rate is used to discount future values back to the present.

In simple terms:

> Risk-free rate = reference interest rate used to bring future money into present value.

---

## 10. What is volatility?

**Volatility** measures how much the price of an asset moves.

An asset with low volatility has smaller price movements.
An asset with high volatility has larger price movements.

Examples:

* 20% annual volatility is represented as 0.20.
* 35% annual volatility is represented as 0.35.

In general, higher volatility can make an option more valuable.

This happens because options benefit from large market movements. If the asset price moves a lot, there is a higher chance that the option ends up in a favorable situation.

In simple terms:

> Volatility = expected uncertainty or movement of the asset price.

---

## 11. What is Black-Scholes?

**Black-Scholes** is a mathematical model used to estimate the theoretical price of European options.

The model receives the following inputs:

* Current asset price.
* Strike price.
* Time to expiration.
* Risk-free rate.
* Volatility.
* Option type: Call or Put.

And returns:

* Theoretical option price.
* Sensitivities called Greeks.

In simple terms:

> Black-Scholes is a formula that estimates how much a European option should be worth under certain assumptions.

---

## 12. What is dividend yield?

Some assets, like certain stocks, pay dividends.

The **dividend yield** represents the expected return from dividends.

In Black-Scholes with dividends, this value is represented as a continuous annual rate.

Examples:

* 0% per year is represented as `0.0`.
* 1.5% per year is represented as `0.015`.
* 3% per year is represented as `0.03`.

If `dividendYield` is not provided, NexusXVA uses `0.0`.
This preserves compatibility with the no-dividend case.

In simple terms:

> Dividend yield = expected dividend return used in the valuation.

---

## 13. What are the Greeks?

The **Greeks** are measures that explain how the option price changes when a market variable changes.

They are not prices. They are sensitivities.

They help explain the behavior of the option.

The main Greeks in this module are:

* Delta.
* Gamma.
* Vega.
* Theta.
* Rho.

In simple terms:

> Greeks help explain why the price of an option changes.

---

## 14. What is Delta?

**Delta** measures how much the option price changes when the price of the underlying asset changes.

Example:

* If a Call option has a Delta of 0.60, it means that if the asset price increases by 1 unit, the option price may increase by approximately 0.60.

For Call options, Delta is usually between 0 and 1.

For Put options, Delta is usually between -1 and 0.

In simple terms:

> Delta measures how sensitive the option is to the asset price.

---

## 15. What is Gamma?

**Gamma** measures how much Delta changes when the asset price changes.

Delta measures the sensitivity of the option price.
Gamma measures the sensitivity of Delta.

Simple example:

* If Gamma is high, Delta can change quickly.
* If Gamma is low, Delta changes more slowly.

In simple terms:

> Gamma measures how fast Delta changes.

---

## 16. What is Vega?

**Vega** measures how much the option price changes when volatility changes.

Example:

* If volatility increases, the option value usually increases as well.
* Vega helps measure that impact.

Even though it is called “Vega”, it is not actually a Greek letter. It is still included in the Greeks by financial convention.

In simple terms:

> Vega measures how sensitive the option is to changes in volatility.

---

## 17. What is Theta?

**Theta** measures how much the option price changes as time passes.

Options usually lose value as they get closer to expiration.

This is known as time decay.

Example:

* An option with a lot of time remaining can be more valuable.
* The same option close to expiration can be less valuable if the market has not moved favorably.

In simple terms:

> Theta measures the effect of time passing on the option price.

---

## 18. What is Rho?

**Rho** measures how much the option price changes when the risk-free interest rate changes.

Example:

* If the risk-free rate increases, the price of a Call option may change.
* Rho measures that impact.

In simple terms:

> Rho measures how sensitive the option is to interest rate changes.

---

## 19. What does stateless calculation mean?

A **stateless** calculation means that no information is stored between one execution and another.

Each calculation works independently.

The logic is:

1. Input data is received.
2. The data is validated.
3. The option price and Greeks are calculated.
4. The result is returned.
5. Nothing is stored in a database.

In simple terms:

> Stateless = each request is calculated independently and leaves no saved state behind.

---

## 20. Complete conceptual example

Suppose we want to calculate a Call option with these inputs:

* Spot: 100
* Strike: 110
* Maturity: 1 year
* Risk-free rate: 5%
* Volatility: 20%
* Type: Call

The interpretation would be:

> We want to estimate how much it is worth today to have the right to buy an asset at 110 one year from now, knowing that the asset is worth 100 today, the risk-free rate is 5%, and the expected volatility is 20%.

The Black-Scholes model takes those inputs and calculates:

* Theoretical option price.
* Delta.
* Gamma.
* Vega.
* Theta.
* Rho.

---

## 21. Summary of main terms

| Term            | Simple explanation                                         |
| --------------- | ---------------------------------------------------------- |
| Option          | Contract that gives the right to buy or sell an asset      |
| Call            | Right to buy                                               |
| Put             | Right to sell                                              |
| European Option | Option that can only be exercised at expiration            |
| Spot            | Current price of the asset                                 |
| Strike          | Fixed price to buy or sell in the future                   |
| Maturity        | Time until expiration                                      |
| Risk-free rate  | Reference rate considered safe                             |
| Volatility      | How much the asset price moves                             |
| Black-Scholes   | Model used to calculate theoretical European option prices |
| Greeks          | Sensitivity measures of the option price                   |
| Delta           | Sensitivity to the asset price                             |
| Gamma           | Sensitivity of Delta                                       |
| Vega            | Sensitivity to volatility                                  |
| Theta           | Sensitivity to time passing                                |
| Rho             | Sensitivity to interest rates                              |
| Stateless       | Independent calculation with no stored data                |

---

## 22. What is a portfolio?

A **portfolio** is a group of financial positions.

In NexusXVA, a European option position stores the trade terms:

* Underlying symbol.
* Option type: Call or Put.
* Strike.
* Maturity date.
* Quantity.

In simple terms:

> Portfolio = a book or group of positions we want to inspect, price, and later use for risk.

---

## 23. What does Quantity mean?

**Quantity** tells us how many units of a position we hold.

Examples:

* `quantity = 10` means a long position of 10 units.
* `quantity = -3` means a short position of 3 units.
* `quantity = 0` does not make sense as a position and is rejected.

When pricing the portfolio:

```text
positionPrice = unitPrice * quantity
```

Greeks are also scaled by quantity.
That is why a short position can have negative position-level value and sensitivities.

---

## 24. What is market data?

**Market data** is the market state used to value a position.

For Black-Scholes we need:

* `spot`: current price of the underlying.
* `volatility`: volatility used by the model.
* `riskFreeRate`: risk-free rate used by the model.
* `dividendYield`: expected dividend return used by the model.

NexusXVA does not store these values inside positions.
The position stores the trade; market data describes the market at a specific time.

In simple terms:

> The portfolio says what we hold. Market data says which prices and rates we use to value it.

---

## 25. What does portfolio pricing do?

Portfolio pricing takes already persisted positions and calculates their theoretical value with Black-Scholes.

The financial flow is:

```text
persisted positions
  + marketdata pricing inputs
  -> Black-Scholes per position
  -> price and Greeks scaled by quantity
  -> portfolio total
```

The individual formula is still the same.
What changes is that the system now:

* Reads many positions.
* Requests market inputs per symbol.
* Converts `maturityDate` into time in years using ACT/365.
* Prices each live position.
* Adds prices and Greeks to produce portfolio totals.

Pricing results are not stored in the database in V1.
They are a stateless valuation for one date and one set of market inputs.

---

## 26. Why V1 prices only USD portfolios

A portfolio can have a `baseCurrency`, but correctly pricing multiple currencies requires **FX conversion**.

Example:

* One position is worth 100 USD.
* Another position is worth 100 EUR.

We cannot directly add:

```text
100 USD + 100 EUR != 200 USD
```

To do it correctly we need an FX rate, such as EUR/USD, and a clear conversion policy.
Because that is not implemented yet, portfolio pricing V1 accepts only `USD` portfolios and `USD` market data.

This avoids returning incorrect totals.
It does not mean NexusXVA can only store USD portfolios; it means the current portfolio pricing endpoint only calculates USD totals until FX is implemented.

---

## 27. What happens with expired positions?

NexusXVA allows positions with past `maturityDate` because a portfolio may contain history or expired trades.

But Black-Scholes with Greeks requires strictly positive time to maturity.
That is why, in portfolio pricing:

* If `maturityDate > valuationDate`, the position is priced.
* If `maturityDate <= valuationDate`, the position is reported as `UNPRICEABLE_EXPIRED`.
* Expired positions are not included in totals.

This avoids mixing Black-Scholes with exact expiry payoff before we have fuller instrument and payoff modeling.

---

## 28. Cases We Explicitly Avoid For Now

The current version avoids some cases on purpose:

* FX and multi-currency portfolios.
* Real implied volatility and volatility surfaces.
* Real options chains.
* Exact payoff at expiration.
* Persisted valuation results.
* Real market data storage inside NexusXVA.
* Real credit curves, counterparties, netting sets, collateral, and wrong-way risk.

These cases are not forgotten.
They are out of scope so the current slice stays small, correct, and testable.

---

## 29. What is Exposure?

**Exposure** measures how much value could be at risk against a counterparty in the future.

NexusXVA Exposure V1 simulates future prices, reprices the portfolio at future dates, and aggregates:

* Expected Exposure.
* Expected Negative Exposure.
* Potential Future Exposure.

In simple terms:

> Exposure estimates how much the portfolio could be worth in future scenarios.

---

## 30. What is CVA?

**Credit Valuation Adjustment**, or **CVA**, estimates the value adjustment caused by counterparty default risk.

NexusXVA CVA V1 uses a simplified formula:

```text
CVA = LGD * sum(discountFactor * expectedExposure * defaultProbabilityIncrement)
```

Where:

* `LGD` means loss given default.
* `expectedExposure` comes from Exposure V1.
* `defaultProbabilityIncrement` comes from a flat hazard rate.
* `discountFactor` brings the contribution back to present value.

This is not a full bank-grade CVA model.
It is the first stateless slice that connects portfolio, market data, exposure, and credit adjustment.

In simple terms:

> CVA estimates how much value we subtract because the counterparty might default while we have positive exposure.

---

## 31. General idea of the module

This module aims to implement a simple but real financial calculation.

It does not try to simulate a complete financial system.
It keeps market data outside NexusXVA and stores trade terms in portfolios.

The first version focuses on:

* Receiving basic European option data.
* Validating that the data makes sense.
* Calculating the theoretical price using Black-Scholes.
* Calculating the main Greeks.
* Returning a clear result.
* Pricing USD portfolios with persisted European option positions using market-data inputs.
* Simulating exposure profiles.
* Calculating simplified CVA from exposure and flat credit assumptions.

In simple terms:

> The module transforms basic financial inputs into a theoretical option price and its main sensitivities.

Important note:

> This endpoint works with strictly positive maturity. Exact payoff at expiration will be modeled separately when the project has a fuller representation of instruments and payoffs.
