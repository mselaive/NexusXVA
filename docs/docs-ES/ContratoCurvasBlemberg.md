# Contrato de Curvas entre Blemberg y NexusXVA

NexusXVA es responsable del ciclo de vida, versionado, aprobación y uso de curvas CVA. Blemberg es responsable de observaciones de mercado y de construir curvas a partir de esas observaciones.

Toda curva recibida desde Blemberg debe ingresar a NexusXVA como `DRAFT`. Nunca se aprueba ni activa automáticamente.

NexusXVA conserva `asOf`, referencia de source, método de construcción y el indicador stale de la curva importada.

## Contrato V1 requerido

```http
GET /api/market-data/curves/discount?currency=USD&valuationDate=2026-07-17
```

La respuesta debe incluir `currency`, `valuationDate`, `name`, `asOf`, `source`, `method`, `stale` y puntos `{date, discountFactor}`.

Reglas:

- Fechas estrictamente crecientes y posteriores a valuation date.
- Discount factors finitos dentro de `(0, 1]`.
- Blemberg usa datos cacheados; no llama al proveedor durante el request de NexusXVA.
- Datos inexistentes devuelven `404` limpio y fallos de servicio `503` limpio.

## Import en NexusXVA

ADMIN usa **XVA Setup**, selecciona moneda, valuation date y decide explícitamente si acepta datos stale. Nexus llama Blemberg y crea una curva inactiva `DRAFT / MARKET_DATA`. Si Blemberg devuelve `stale=true`, la importación responde `409` salvo que ADMIN haya habilitado `allowStale`.

La curva debe aprobarse por separado antes de aparecer en CVA.

## Curvas de crédito

Una curva de crédito necesita observaciones defendibles, por ejemplo spreads CDS, spreads de bonos o un modelo interno explícito basado en rating. No debe derivarse artificialmente desde acciones o tasas Treasury.

Blemberg ya expone un proxy crediticio USD investment-grade construido desde OAS corporativo por rating:

```http
GET /api/market-data/curves/credit?creditRating=A&currency=USD&valuationDate=2026-08-02&recoveryRate=0.40
```

Nexus obtiene el rating desde la counterparty, conserva spread, recovery, hazard proxy, rating bucket, serie fuente, fecha de observación y staleness, y crea una curva inactiva `DRAFT / MARKET_DATA`. Es un proxy agregado por rating, no una curva CDS específica del issuer. Los formatos CSV y el JSON completo están documentados en [BlembergCurveContract.md](../docs-EN/BlembergCurveContract.md).
