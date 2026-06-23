# Proceso EOD en NexusXVA

## Objetivo

EOD significa **End of Day**. Su objetivo es guardar una referencia oficial e inmutable del valor de cada portfolio al cierre.

EOD no modifica:

- El premium original del trade.
- Los terminos de posiciones.
- Bookings o lifecycle.
- Market data en Blemberg.

Solo guarda una fotografia del pricing utilizado al cierre.

## Responsabilidades

- **FO** consulta Daily P&L y Unrealized P&L, pero no ejecuta cierres.
- **BO** puede ejecutar un cierre manual desde `EOD Control`.
- **SYSTEM** puede ejecutar cierres automaticos mediante el scheduler.
- **ADMIN** configura usuarios y permisos, pero no cierra portfolios por defecto.

## Flujo

```text
Blemberg actualiza market data
  -> BO selecciona business date
  -> NexusXVA recorre todos los portfolios
  -> para cada portfolio corre Pricing y controles de calidad
  -> guarda portfolio_eod_runs y position snapshots
  -> reporta CAPTURED, SKIPPED o FAILED por portfolio
```

## Como Ejecutarlo Manualmente

1. Entrar con un usuario BO, por ejemplo `bo.pnl`.
2. Abrir `EOD Control`.
3. Elegir la fecha de negocio.
4. Presionar `Run EOD for all portfolios`.
5. Confirmar la operacion global.
6. Revisar el resultado de cada portfolio.
7. Usar `Portfolio close history` para inspeccionar un libro concreto.

Solo puede existir un cierre por portfolio y business date.

## Que Debemos Esperar

Si funciona correctamente:

- Aparece un resumen con portfolios capturados, omitidos y fallidos.
- `CAPTURED` indica que se creo el cierre.
- `SKIPPED` normalmente indica que ese portfolio ya estaba cerrado para la fecha.
- `FAILED` requiere revisar market data o posiciones no valorables.
- Se muestran market value, trade value y unrealized P&L.
- La pantalla Pricing de FO usa ese cierre para Daily P&L.
- El campo `source` indica `MANUAL_BO_BATCH`, `SCHEDULED` o `DEMO_SEED`.

## Daily P&L

Para una posicion que existia en el ultimo cierre:

```text
dailyPnl = currentMarketValue - priorEodMarketValue
```

Para una posicion creada despues del cierre:

```text
dailyPnl = currentMarketValue - executionTradeValue
```

Si no existe EOD ni execution price:

```text
referenceMethod = UNAVAILABLE
dailyPnl = null
```

Unrealized P&L sigue comparando contra el trade original:

```text
unrealizedPnl = currentMarketValue - executionTradeValue
```

## Errores Esperados

- `EOD snapshot already exists`: el portfolio ya fue cerrado para esa fecha.
- `EOD snapshot cannot use stale market data`: Blemberg debe refrescarse antes del cierre.
- `EOD snapshot requires all active positions to be priceable`: existe una posicion vencida o sin inputs validos.
- `Market data service unavailable`: Blemberg no responde.
- Portfolio no USD: fuera del alcance de Pricing/EOD V1.

Un error no crea un snapshot parcial para ese portfolio. Los demas portfolios continúan: el batch no pierde cierres correctos por el fallo de un libro.

## Scheduler

El scheduler esta deshabilitado por defecto:

```text
NEXUSXVA_EOD_ENABLED=false
```

Configuracion recomendada:

```text
NEXUSXVA_EOD_ENABLED=true
NEXUSXVA_EOD_CRON=0 15 17 * * MON-FRI
NEXUSXVA_EOD_ZONE=America/New_York
NEXUSXVA_EOD_ALLOW_STALE=false
```

Esto ejecuta el cierre a las 17:15, de lunes a viernes, usando la fecha de negocio de Nueva York.

Para desarrollo conviene mantenerlo apagado y usar `EOD Control`.

## Datos Demo

Usuarios:

- `fo.tech / fo12345`
- `fo.macro / fo12345`
- `bo.pnl / bo12345`

Portfolios:

- `P&L Demo - Tech Options`
- `P&L Demo - US Banks`
- `P&L Demo - Macro Hedges`

Estos portfolios incluyen execution premiums y un cierre anterior creado como `DEMO_SEED`.
