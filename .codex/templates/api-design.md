# API Design Template

## API Name

Name the API or endpoint group.

## Purpose

Describe the workflow this API supports.

## Consumers

- Backend internal use.
- Frontend dashboard.
- Future CLI or integration.

## Endpoint Summary

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/example` | Describe action |

## Request Contract

```json
{
  "field": "value"
}
```

## Response Contract

```json
{
  "field": "value"
}
```

## Validation Rules

- Rule 1.
- Rule 2.

## Error Responses

| Status | Reason | Example |
| --- | --- | --- |
| 400 | Invalid input | Negative volatility |
| 404 | Resource missing | Portfolio not found |
| 500 | Unexpected failure | Internal error |

## Idempotency

Describe whether repeated requests should create new resources or return equivalent results.

## Security Considerations

Describe current and future security expectations.

## Observability

Describe logs, metrics, timings, and identifiers.

## Backward Compatibility

Describe fields that must remain stable and migration concerns.

