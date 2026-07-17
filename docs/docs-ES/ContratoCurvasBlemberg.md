# Contrato de Curvas entre Blemberg y NexusXVA

NexusXVA es responsable del ciclo de vida, versionado, aprobación y uso de curvas CVA. Blemberg es responsable de observaciones de mercado y de construir curvas a partir de esas observaciones.

Toda curva recibida desde Blemberg debe ingresar a NexusXVA como `DRAFT`. Nunca se aprueba ni activa automáticamente.

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

## Curvas de crédito

Una curva de crédito necesita observaciones defendibles, por ejemplo spreads CDS, spreads de bonos o un modelo interno explícito basado en rating. No debe derivarse artificialmente desde acciones o tasas Treasury.

Hasta que Blemberg tenga una fuente adecuada, las curvas de crédito ingresan mediante CSV revisado o creación manual. Los formatos CSV y el JSON completo esperado están documentados en [BlembergCurveContract.md](../docs-EN/BlembergCurveContract.md).
