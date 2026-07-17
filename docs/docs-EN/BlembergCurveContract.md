# Blemberg Curve Contract for NexusXVA

## Purpose

NexusXVA owns CVA curve lifecycle, versions, approvals and usage. Blemberg owns market observations and curve construction from those observations.

Market-data curves received from Blemberg must always become `DRAFT` curves in NexusXVA. They are never approved or activated automatically.

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

## Credit Curves

Credit curves require counterparty credit observations such as CDS spreads, bond spreads or an explicitly configured internal rating model. Blemberg must not fabricate them from equity prices or Treasury rates.

A future endpoint may be added when a defensible source exists:

```http
GET /api/market-data/curves/credit?externalId=DPB-001&valuationDate=2026-07-17
```

The response must identify the observation type, recovery/LGD assumption, construction method and source. Until then, credit curves enter NexusXVA through reviewed CSV imports or manual drafts.

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
