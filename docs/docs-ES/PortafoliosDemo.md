# Portafolios Demo

NexusXVA incluye un seed SQL opcional para crear portafolios grandes de demo:

```text
backend/src/main/resources/db/demo/demo_portfolios.sql
```

Estos portafolios no son migraciones Flyway. Se cargan solo cuando un dev quiere una base local con datos ricos para demos, QA manual, pricing, exposure, CVA, pre-trade analysis y stress testing.

## Como Cargarlos

Con Docker Compose corriendo:

```bash
docker compose exec -T postgres psql -U nexusxva -d nexusxva < backend/src/main/resources/db/demo/demo_portfolios.sql
```

El script es idempotente por UUID fijo. Si lo corres de nuevo, actualiza los mismos libros y posiciones demo sin duplicarlos.

## Que Crea

- `Demo - Mega Cap AI Options Book`: AAPL, MSFT, NVDA, AMZN, GOOGL, META, TSLA, AVGO, ORCL y AMD.
- `Demo - US Banks Rates Book`: JPM, BAC, GS, MS, C y WFC, con coberturas SPY/QQQ/TLT.
- `Demo - Index Macro Hedge Book`: SPY, QQQ, DIA, IWM, VTI y TLT.
- `Demo - Metals Inflation Hedge Book`: GLD, SLV y CPER, con coberturas de equity y duracion.
- `Demo - Cross Asset FO Test Book`: mezcla de tecnologia, bancos, ETFs, metales y duracion.

El seed crea 5 portafolios USD y 72 posiciones confirmadas de opciones europeas. Las posiciones ya estan confirmadas para que pricing, exposure, CVA y stress testing puedan correr altiro sin aprobacion BO.

## Razonamiento

Los simbolos estan restringidos a la watchlist V1 de NexusXVA/Blemberg. Asi el demo funciona tanto con el provider local de market data como con los pricing inputs de Blemberg.

Los strikes estan alineados con los spots del mock local de market data, usando una mezcla de posiciones ATM, OTM y posiciones tipo hedge. Las cantidades incluyen largos y cortos para que Greeks, stress impacts y exposure sean mas interesantes que un libro en una sola direccion.

Estos son portafolios demo, no recomendaciones de inversion ni estrategias oficiales.
