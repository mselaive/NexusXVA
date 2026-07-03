# Portafolios Demo

NexusXVA incluye un seed SQL opcional para crear portafolios grandes de demo:

```text
backend/src/main/resources/db/demo/demo_portfolios.sql
```

Tambien incluye un seed opcional para workflow:

```text
backend/src/main/resources/db/demo/demo_workflows.sql
```

Para pruebas locales mas pesadas, NexusXVA tambien incluye:

```text
backend/src/main/resources/db/demo/demo_heavy_portfolios.sql
```

Estos portafolios no son migraciones Flyway. Se cargan solo cuando un dev quiere una base local con datos ricos para demos, QA manual, pricing, exposure, CVA, pre-trade analysis y stress testing.

## Como Cargarlos

Con Docker Compose corriendo:

```bash
docker compose exec -T postgres psql -U nexusxva -d nexusxva < backend/src/main/resources/db/demo/demo_portfolios.sql
```

Para cargar libros demo de workflow:

```bash
docker compose exec -T postgres psql -U nexusxva -d nexusxva < backend/src/main/resources/db/demo/demo_workflows.sql
```

Para cargar portafolios mas pesados:

```bash
docker compose exec -T postgres psql -U nexusxva -d nexusxva < backend/src/main/resources/db/demo/demo_heavy_portfolios.sql
```

El script es idempotente por UUID fijo. Si lo corres de nuevo, actualiza los mismos libros y posiciones demo sin duplicarlos.

## Que Crea

- `Demo - Mega Cap AI Options Book`: AAPL, MSFT, NVDA, AMZN, GOOGL, META, TSLA, AVGO, ORCL y AMD.
- `Demo - US Banks Rates Book`: JPM, BAC, GS, MS, C y WFC, con coberturas SPY/QQQ/TLT.
- `Demo - Index Macro Hedge Book`: SPY, QQQ, DIA, IWM, VTI y TLT.
- `Demo - Metals Inflation Hedge Book`: GLD, SLV y CPER, con coberturas de equity y duracion.
- `Demo - Cross Asset FO Test Book`: mezcla de tecnologia, bancos, ETFs, metales y duracion.

El seed crea 5 portafolios USD y 72 posiciones confirmadas de opciones europeas. Las posiciones ya estan confirmadas para que pricing, exposure, CVA y stress testing puedan correr altiro sin aprobacion BO.

El seed de workflow crea:

- `Workflow Demo - FO Tech Intake`
- `Workflow Demo - Macro Approval Queue`
- `Workflow Demo - Metals Lifecycle`

Tambien crea solicitudes de booking en `PENDING_VALIDATION`, `CONFIRMED` y `REJECTED`, mas solicitudes lifecycle en `PENDING_VALIDATION`, `APPROVED` y `REJECTED`.

Para revisarlas:

1. Entra como usuario ADMIN.
2. Abre `Workflows`.
3. Usa el filtro de portfolio para elegir uno de los libros `Workflow Demo - ...`, o deja el filtro vacio para ver todo.
4. Cambia entre `New trade bookings` y `Position lifecycle`.

La misma data sirve desde BO:

- `Trade Validation` muestra trades nuevos y lifecycle requests pendientes.
- `Lifecycle Reporting` muestra presion de cola, aging y breakdown por simbolo/portfolio.

## Portafolios Heavy Demo

El seed pesado crea:

- `Heavy Demo - Mega Tech Vol Warehouse`
- `Heavy Demo - Cross Asset Scenario Grid`
- `Heavy Demo - Metals Macro Hedge Stack`

Crea alrededor de 512 posiciones de opciones europeas y 32 posiciones de cash equity entre los tres libros. Tambien agrega 99 booking requests y 81 lifecycle requests distribuidos entre estados pending, confirmed/approved y rejected para que los libros aparezcan en ambas pestanas del workflow de ADMIN.

Usa estos libros cuando quieras estresar:

- Render de lista/detalle de portfolios con muchas filas.
- Agregacion de portfolio pricing.
- Matrices de Stress Testing.
- Runtime de Exposure/CVA sobre libros mas grandes.
- Delta Hedge con opciones y cash equities.
- Visualizacion de ADMIN workflow para libros grandes con entradas aceptadas, rechazadas y pendientes tanto en `New trade bookings` como en `Position lifecycle`.

Estos son libros sinteticos para pruebas de carga local. Son intencionalmente grandes para QA y no representan portfolios productivos ni recomendaciones.

## Razonamiento

Los simbolos estan restringidos a la watchlist V1 de NexusXVA/Blemberg. Asi el demo funciona tanto con el provider local de market data como con los pricing inputs de Blemberg.

Los strikes estan alineados con los spots del mock local de market data, usando una mezcla de posiciones ATM, OTM y posiciones tipo hedge. Las cantidades incluyen largos y cortos para que Greeks, stress impacts y exposure sean mas interesantes que un libro en una sola direccion.

Estos son portafolios demo, no recomendaciones de inversion ni estrategias oficiales.
