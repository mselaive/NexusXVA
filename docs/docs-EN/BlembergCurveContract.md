# Blemberg Curve Contract for NexusXVA

## Purpose

NexusXVA owns CVA curve lifecycle, versions, approvals and usage. Blemberg owns market observations and curve construction from those observations.

Market-data curves received from Blemberg must always become `DRAFT` curves in NexusXVA. They are never approved or activated automatically.

NexusXVA persists the imported curve lineage:

- `source=MARKET_DATA`
- provider/cache reference from `source`
- provider `asOf`
- construction `method`
- provider `stale` flag

## Required V1 Endpoint

```http
GET /api/market-data/curves/discount?currency=USD&valuationDate=2026-07-17
```

Expected response:

```json
{
  "curveType": "DISCOUNT_FACTOR",
  "currency": "USD",
  "valuationDate": "2026-07-17",
  "name": "USD Risk-Free Discount Curve",
  "asOf": "2026-07-17T21:00:00Z",
  "source": "BLEMberg_CACHE",
  "method": "ZERO_RATE_LINEAR_INTERPOLATION",
  "stale": false,
  "points": [
    { "date": "2026-08-17", "discountFactor": 0.9962 },
    { "date": "2026-10-17", "discountFactor": 0.9887 },
    { "date": "2027-01-17", "discountFactor": 0.9771 },
    { "date": "2027-07-17", "discountFactor": 0.9554 }
  ]
}
```

Rules:

- Dates must be strictly increasing and later than `valuationDate`.
- Discount factors must be finite and in `(0, 1]`.
- `asOf`, `source`, `method` and `stale` are required for traceability.
- Blemberg should derive discount factors from its cached risk-free term structure, not call an upstream provider during the NexusXVA request.
- Missing inputs return a clean `404`; provider/cache failures return a clean `503`.

## NexusXVA Import

ADMIN imports a discount curve from **XVA Setup** by selecting currency, valuation date and whether stale data is explicitly allowed.

```http
POST /api/xva/discount-curves/imports/market-data
Content-Type: application/json

{
  "currency": "USD",
  "valuationDate": "2026-07-31",
  "name": "USD Risk-Free Discount Curve",
  "allowStale": false
}
```

The endpoint calls Blemberg, validates the response and creates an inactive `DRAFT`. A stale response returns `409` unless `allowStale=true`. ADMIN must separately approve the draft before CVA can select it.

## Credit Curves

Blemberg provides a USD investment-grade market proxy built from cached FRED ICE BofA corporate OAS observations by rating bucket:

```http
GET /api/market-data/curves/credit?creditRating=A&currency=USD&valuationDate=2026-08-02&recoveryRate=0.40
```

The response uses `CUMULATIVE_DEFAULT_PROBABILITY` and includes the requested rating, normalized rating bucket, recovery assumption, OAS spread, flat hazard proxy, observation date, FRED series, construction method, stale flag and seven curve points. Supported buckets are AAA, AA, A and BBB; `+` and `-` variants map to their broad bucket.

ADMIN imports it through:

```http
POST /api/xva/credit-curves/imports/market-data
```

NexusXVA obtains the rating from its counterparty and creates an inactive `DRAFT` with `source=MARKET_DATA`. A stale response requires explicit `allowStale=true`. The curve is never approved automatically.

This is a rating-level corporate bond OAS proxy, not issuer-specific CDS or bond calibration. It is suitable for the current internal/demo CVA workflow only; lineage must remain visible during review.

## File Formats Supported by NexusXVA

Credit survival curve:

```csv
date,survivalProbability
2027-01-17,0.9900
2027-07-17,0.9750
2028-07-17,0.9400
```

Credit cumulative-default curve:

```csv
date,cumulativeDefaultProbability
2027-01-17,0.0100
2027-07-17,0.0250
2028-07-17,0.0600
```

Discount curve:

```csv
date,discountFactor
2027-01-17,0.9771
2027-07-17,0.9554
2028-07-17,0.9120
```

Imports create inactive `DRAFT` versions with source `IMPORT`. ADMIN must review and approve them before CVA can use them.
