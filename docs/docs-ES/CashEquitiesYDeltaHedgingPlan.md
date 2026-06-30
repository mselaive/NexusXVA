# Cash Equities y Delta Hedging - Estado V1

Este documento explica como NexusXVA incorpora cash equities y delta hedging sin debilitar el modelo actual.

Estado actual:

- Implementado: tabla separada de cash equities, booking FO/BO, visualizacion en portfolio/u-Pad, inclusion en portfolio pricing y pagina FO Delta Hedge.
- Pendiente: EOD completo por cash equities, lifecycle amend/cancel para cash equities, shortcut automatico desde Delta Hedge hacia u-Pad y un puerto generico de spot en `marketdata`.

## Decision Principal

Cash equities no deben modelarse como opciones con campos vacios. Tendran su propia tabla de posiciones, requests de booking propios o un booking polimorfico bien validado, y su propia logica de valuacion.

La regla sigue siendo la misma:

```text
portfolio = trade terms confirmados
marketdata = spot / rates / vol / dividend yield
pricing = calculo stateless
```

NexusXVA no guardara spot de acciones dentro del portfolio.

## Cash Equity Position V1

Tabla implementada:

```text
portfolio_cash_equity_positions
  id
  portfolio_id
  underlying_symbol
  quantity
  execution_price
  lifecycle_status
  created_at
  updated_at
```

Campos:

- `underlying_symbol`: simbolo normalizado y validado contra Blemberg/local market data.
- `quantity`: numero de acciones. Positivo para long, negativo para short.
- `execution_price`: precio negociado por accion, opcional para posiciones historicas.
- `lifecycle_status`: `ACTIVE`, `CANCELLED`, `AMENDED`.

No se guardan:

- `spot`
- `marketValue`
- `unrealizedPnl`
- datos Blemberg

Esos valores se calculan bajo demanda o se capturan en EOD.

## Valuacion De Cash Equities

Para una posicion cash equity:

```text
marketValue = spot * quantity
tradeValue = executionPrice * quantity
unrealizedPnl = marketValue - tradeValue
deltaShares = quantity
gamma = 0
vega = 0
theta = 0
rho = 0
```

En V1 la fuente de `spot` viene por `marketdata`. Idealmente evolucionara hacia un puerto generico de spot:

```text
portfolio pricing
  -> marketdata.application spot input
  -> Blemberg cached snapshot / pricing input
```

Mientras Blemberg/NexusXVA no tengan un endpoint/puerto generico de spot, NexusXVA reutiliza temporalmente el spot de `pricing-inputs`. Esto es transitorio y no convierte cash equities en opciones.

## Booking FO/BO

FO puede enviar un cash equity booking desde `u-Pad`:

```json
{
  "underlyingSymbol": "AAPL",
  "quantity": 100,
  "executionPrice": 190.25
}
```

BO aprueba o rechaza igual que con opciones:

```text
FO submits cash equity booking
  -> PENDING_VALIDATION
  -> BO approves
  -> portfolio_cash_equity_positions ACTIVE
```

Pricing, Exposure, CVA y EOD solo deben usar posiciones `ACTIVE`.

## Delta Hedging V1

Delta hedging empieza como analisis, no como automatic trading.

Endpoint propuesto:

```text
POST /api/front-office/delta-hedge/european-options
```

Entrada:

```json
{
  "portfolioId": "uuid",
  "valuationDate": "2026-06-30",
  "targetDeltaBySymbol": {
    "AAPL": 0
  }
}
```

Respuesta:

```json
{
  "portfolioId": "uuid",
  "valuationDate": "2026-06-30",
  "rows": [
    {
      "symbol": "AAPL",
      "optionDeltaShares": 420.5,
      "cashEquityDeltaShares": -100.0,
      "netDeltaShares": 320.5,
      "targetDeltaShares": 0.0,
      "suggestedCashEquityQuantity": -320.5,
      "spot": 190.0,
      "estimatedTradeNotional": -60895.0
    }
  ]
}
```

La sugerencia no bookea nada. Si FO quiere ejecutar el hedge, la UI debe poder mandar el ticket a `u-Pad` como cash equity booking y pasar por BO.

## Cambios De UI

`u-Pad` tiene selector de producto:

- European Option
- Option Strategy
- Cash Equity

Portfolio detail separa:

- Option positions
- Cash equity positions
- Historical / inactive positions

Delta Hedge aparece como herramienta FO:

- Seleccionar portfolio.
- Ver delta neta por simbolo.
- Ver hedge sugerido.
- Enviar hedge sugerido a `u-Pad`.

## Riesgos Que Debemos Evitar

- No mezclar cash equities en `portfolio_european_option_positions`.
- No hacer que una accion tenga `strike` o `maturityDate`.
- No permitir que pricing de opciones dependa de snapshots crudos de frontend.
- No convertir delta hedge en auto-booking; siempre debe pasar por FO ticket y BO approval.
- No introducir FX en este slice. V1 sigue USD-only.

## Pendientes Recomendados

1. Incluir cash equities en EOD snapshots y Daily P&L por posicion.
2. Agregar lifecycle amend/cancel para cash equities.
3. Agregar shortcut de hedge sugerido hacia `u-Pad`.
4. Reemplazar el uso temporal de `pricing-inputs` como fuente de spot por un puerto generico de spot.
5. Crear tests financieros mas especificos para delta hedge con opciones + cash equity.
