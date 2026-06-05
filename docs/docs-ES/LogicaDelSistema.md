# Logica del sistema NexusXVA

Este documento explica como va funcionando la logica del sistema mientras lo construimos.
No reemplaza al README ni a los tests: sirve como mapa vivo para que otros devs entiendan por que el codigo esta organizado asi y que decision tomamos en cada etapa.

## Camino elegido

NexusXVA se esta construyendo como un modular monolith.
Eso significa que tenemos una sola aplicacion backend, pero separada internamente por modulos claros.

La razon es simple:

- Primero necesitamos demostrar correctness financiera y buen diseno backend.
- Todavia no necesitamos microservicios, Kafka ni ejecucion distribuida.
- Cada feature debe poder probarse y explicarse como un vertical slice pequeno.

El orden elegido es:

1. Backend foundation.
2. Pricing stateless de opciones europeas.
3. Portfolio management.
4. Pricing de portfolio.
5. Monte Carlo simulation.
6. Exposure analytics.
7. CVA simplificado.
8. Dashboard.

La idea es que cada paso use lo anterior, sin adelantarse demasiado.

## Estado actual

Actualmente el backend tiene:

- Health endpoint.
- Manejo de errores API estable.
- Estructura modular por paquetes.
- Pricing stateless de opciones europeas con Black-Scholes.
- Calculo de Greeks principales.
- Portfolio management persistido en PostgreSQL.
- Pricing stateless de portfolio con Black-Scholes.
- Integracion Blemberg para validar simbolos y pedir pricing inputs.
- Inputs temporales de market data local como fallback de desarrollo.
- Exposure V1 con Monte Carlo GBM simple y repricing Black-Scholes.
- Migraciones de base de datos con Flyway.
- Tests financieros, de API y de persistencia.

Los endpoints principales actuales son:

```text
POST /api/pricing/european-options/black-scholes
POST /api/portfolios
GET /api/portfolios
GET /api/portfolios/{portfolioId}
PATCH /api/portfolios/{portfolioId}
DELETE /api/portfolios/{portfolioId}
POST /api/portfolios/{portfolioId}/instruments/european-options
GET /api/portfolios/{portfolioId}/instruments
GET /api/portfolios/{portfolioId}/instruments/{positionId}
PATCH /api/portfolios/{portfolioId}/instruments/european-options/{positionId}
DELETE /api/portfolios/{portfolioId}/instruments/{positionId}
POST /api/portfolios/{portfolioId}/pricing/black-scholes
POST /api/simulations/exposure
```

El endpoint de pricing recibe datos de una opcion europea y devuelve:

- Precio teorico.
- Delta.
- Gamma.
- Vega.
- Theta.
- Rho.

Los endpoints de portfolio permiten:

- Crear portfolios.
- Listar portfolios con resumen y cantidad de posiciones.
- Recuperar un portfolio por id.
- Actualizar metadata de portfolio.
- Borrar portfolios.
- Agregar posiciones de opciones europeas.
- Listar instrumentos de un portfolio.
- Obtener, actualizar y borrar posiciones individuales.
- Valorar posiciones europeas persistidas a nivel portfolio usando Black-Scholes.
- Simular perfiles de exposure futuros para portfolios persistidos.

## Como fluye una request de pricing

El flujo actual es:

```text
HTTP request
  -> pricing.api
  -> pricing.application
  -> pricing.domain
  -> HTTP response
```

En mas detalle:

1. El controller recibe JSON.
2. El DTO valida campos obligatorios y rangos basicos.
3. El DTO se transforma a un input de dominio.
4. El servicio de aplicacion ejecuta el caso de uso.
5. El calculator puro aplica Black-Scholes.
6. El dominio valida que el resultado sea finito.
7. La API devuelve precio y Greeks.

Esto mantiene la formula financiera fuera de Spring MVC.
Esa separacion importa porque la matematica debe poder probarse sin levantar la aplicacion.

## Como fluye una request de portfolio

El flujo actual de portfolio es:

```text
HTTP request
  -> portfolio.api
  -> portfolio.application
  -> marketdata.application
  -> Blemberg REST API (si validacion esta habilitada)
  -> portfolio.infrastructure
  -> PostgreSQL
  -> HTTP response
```

En mas detalle:

1. El controller recibe JSON.
2. El DTO valida campos obligatorios y rangos basicos.
3. El DTO se transforma a un comando de aplicacion.
4. El servicio de aplicacion valida el simbolo contra market data si la integracion esta habilitada.
5. El puerto `PortfolioStore` abstrae la persistencia.
6. El adaptador JPA persiste o recupera entidades.
7. La API devuelve DTOs, no entidades JPA.

Esto permite tener persistencia real sin mezclar reglas HTTP, reglas de dominio y detalles de base de datos.

## Como fluye una request de pricing de portfolio

El flujo actual de pricing de portfolio es:

```text
HTTP request
  -> portfolio.api
  -> portfolio.application
  -> marketdata.application pricing inputs
  -> pricing.domain Black-Scholes
  -> aggregated portfolio valuation
  -> HTTP response
```

En mas detalle:

1. El controller recibe el portfolio id y un `valuationDate` opcional.
2. El servicio de aplicacion carga el portfolio persistido y sus posiciones.
3. El servicio pide a `marketdata` `spot`, `volatility`, `riskFreeRate` y `dividendYield` para cada simbolo valorable.
4. El servicio convierte `maturityDate` a `timeToMaturityYears` usando ACT/365.
5. El calculator Black-Scholes existente valora cada posicion viva.
6. Precio y Greeks se escalan por `quantity`.
7. Las posiciones vencidas vuelven como `UNPRICEABLE_EXPIRED` y quedan fuera de los totales.

Esto sigue siendo pricing stateless.
El portfolio guarda terminos del trade, el modulo market-data entrega inputs de valoracion y los resultados de pricing no se persisten.

## Como fluye una request de exposure

El flujo actual de exposure es:

```text
HTTP request
  -> simulation.api
  -> exposure.application
  -> portfolio.application / portfolio.infrastructure
  -> marketdata.application pricing inputs
  -> simulation.domain GBM paths
  -> pricing.domain Black-Scholes repricing
  -> exposure.domain aggregation
  -> HTTP response
```

En mas detalle:

1. El controller recibe parametros de simulacion como `portfolioId`, `valuationDate`, `horizonDays`, `timeSteps`, `paths`, `seed` y `pfeConfidenceLevel`.
2. El servicio de aplicacion carga el portfolio persistido y sus posiciones vivas.
3. El servicio pide un set de pricing inputs por underlying a traves de `marketdata`.
4. El generador GBM simula precios futuros usando `spot`, `riskFreeRate`, `dividendYield` y `volatility`.
5. Cada posicion viva se repricing en cada fecha futura con Black-Scholes.
6. Las posiciones vencidas se excluyen desde la fecha futura donde `maturityDate <= simulatedDate`.
7. El agregador devuelve expected exposure, expected negative exposure y PFE por fecha.

Exposure V1 es sincrono y stateless.
No persiste paths, market data ni resultados de exposure.

## Responsabilidad de cada capa

### `pricing.api`

Se encarga del contrato HTTP:

- URL.
- Request JSON.
- Response JSON.
- Validacion de borde con annotations.
- Traduccion entre DTOs y dominio.

No debe contener formulas financieras.

### `pricing.application`

Se encarga de orquestar el caso de uso.

Por ahora es simple: recibe el input y llama al calculator.
Mas adelante podria coordinar portfolio-level pricing, observabilidad o integracion con persistencia.

### `pricing.domain`

Contiene la logica financiera pura.

Aqui viven:

- Inputs de dominio.
- Resultados.
- Greeks.
- Calculator Black-Scholes.
- Invariantes financieras.

Esta capa no debe depender de controllers, HTTP, JSON ni base de datos.

### `portfolio.api`

Se encarga del contrato HTTP de portfolios:

- Crear portfolio.
- Listar portfolios.
- Obtener portfolio por id.
- Actualizar portfolio.
- Borrar portfolio.
- Agregar opcion europea a portfolio.
- Listar instrumentos del portfolio.
- Obtener, actualizar y borrar posiciones individuales.

No debe devolver entidades JPA directamente.

### `portfolio.application`

Contiene los casos de uso y las transacciones.

Por ahora coordina:

- Crear portfolio.
- Listar portfolios.
- Actualizar metadata.
- Borrar portfolios y sus posiciones.
- Validar que un portfolio exista antes de agregar/listar posiciones.
- Agregar posiciones de opciones europeas.
- Operar posiciones individuales.
- Valorar un portfolio con Black-Scholes sin persistir resultados de valoracion.

### `portfolio.domain`

Contiene las reglas del portfolio y las posiciones.

Aqui se valida que:

- El portfolio tenga nombre.
- La descripcion sea opcional y corta.
- La base currency sea un codigo de 3 letras, con default `USD`.
- El simbolo del underlying exista y se normalice.
- El strike sea mayor que cero.
- La quantity sea distinta de cero.

### `portfolio.infrastructure`

Contiene JPA entities, repositories y el adaptador que implementa `PortfolioStore`.

Esta capa conoce PostgreSQL/JPA; las capas API, application y domain no deben depender de esos detalles.

### `marketdata.application`

Contiene los puertos internos para validar instrumentos y pedir pricing inputs de opciones europeas.

Portfolio y exposure deben hablar con `marketdata.application`, no directamente con los adapters de Blemberg.

### `marketdata.infrastructure`

Contiene providers externos y temporales de market data:

- Adapters REST de Blemberg.
- Adapter local de watchlist/pricing inputs para desarrollo.

No debe persistir market data dentro de NexusXVA.

### `simulation.api`

Contiene el contrato HTTP de simulacion de exposure.

Valida el request y traduce DTOs a comandos de aplicacion.

### `simulation.domain`

Contiene logica pura de simulacion, como generacion deterministica de paths GBM.

Debe poder probarse sin Spring, HTTP ni base de datos.

### `exposure.application`

Orquesta el caso de uso de exposure:

- Cargar posiciones del portfolio.
- Pedir pricing inputs a market data.
- Generar paths simulados de spot.
- Repricear posiciones en la grilla temporal.
- Devolver un resultado stateless de exposure.

### `exposure.domain`

Contiene la logica de agregacion de exposure:

- Expected Exposure.
- Expected Negative Exposure.
- Potential Future Exposure.

## Decisiones financieras actuales

Para el primer pricing slice decidimos:

- Soportar solo opciones europeas `CALL` y `PUT`.
- Usar Black-Scholes clasico.
- Usar volatilidad constante.
- Usar tasa libre de riesgo constante y continuamente compuesta.
- Soportar `dividendYield` continuo opcional, con default `0.0`.
- Usar decimales para tasas y volatilidad: `0.05` significa 5%.
- Permitir tasa libre de riesgo negativa si el resultado sigue siendo finito.
- Exigir `timeToMaturityYears > 0`.
- Mantener `OptionType` en `instruments.domain` porque es compartido por pricing y portfolio.

El caso exacto de vencimiento (`timeToMaturityYears = 0`) queda fuera del endpoint actual.
La razon es que en vencimiento el precio pasa a ser payoff intrinseco y varios Greeks pueden ser discontinuos o indefinidos.
Ese caso se modelara mejor cuando tengamos instrumentos/payoffs mas explicitos.

## Decisiones de portfolio actuales

Para el primer slice de portfolio decidimos:

- Persistir portfolios en PostgreSQL.
- Persistir posiciones de opciones europeas.
- Usar UUID como identificador publico y de base de datos.
- Guardar metadata de portfolio: `name`, `description`, `baseCurrency`, `createdAt` y `updatedAt`.
- Guardar `underlyingSymbol`, `optionType`, `strike`, `maturityDate`, `quantity`, `createdAt` y `updatedAt`.
- Permitir quantity positiva o negativa para representar posiciones long/short.
- Rechazar quantity igual a cero.
- Permitir `maturityDate` en el pasado; pricing decidira si puede valorar trades vencidos.
- No guardar `spot`, `volatility` ni `riskFreeRate` dentro de la posicion.
- Validar `underlyingSymbol` contra Blemberg cuando `nexusxva.market-data.validation.enabled=true`.
- Permitir `nexusxva.market-data.provider=local` como watchlist y provider temporal de pricing inputs para desarrollo.
- Valorar portfolios solo en `USD` para V1; FX conversion no esta implementado todavia.
- Usar Blemberg como fuente real objetivo de pricing inputs.
- Usar el provider local solo como fuente temporal de inputs demo.
- Mantener el pricing de portfolio stateless; no persistir resultados de valoracion.

La separacion trade/market data es intencional.
Un portfolio describe que instrumentos tenemos; market data describe el estado del mercado usado para valorar esos instrumentos.
Por eso NexusXVA mantiene las posiciones como terminos del trade y usa el modulo `marketdata` como frontera para hablar con Blemberg.
Blemberg valida existencia del instrumento y entrega `spot`, `volatility`, `riskFreeRate` y `dividendYield` reales.
El provider local valida simbolos conocidos y entrega inputs demo temporales de pricing, pero no persiste market data ni reemplaza Blemberg.

## Decisiones actuales de exposure

Para Exposure V1 decidimos:

- Soportar solo portfolios persistidos de opciones europeas.
- Usar un modelo GBM simple para paths futuros de spot.
- Reutilizar Black-Scholes para repricing en fechas futuras.
- Usar `spot`, `volatility`, `riskFreeRate` y `dividendYield` desde `marketdata`.
- Mantener simulaciones deterministicas cuando se entrega un `seed` fijo.
- Devolver expected exposure, expected negative exposure y PFE por fecha.
- Excluir posiciones cuando ya estan vencidas para una fecha futura simulada.
- Mantener exposure stateless y sincrono.
- Mantener USD-only hasta implementar conversion FX.

Este es el puente entre portfolio pricing y CVA.
CVA debe consumir un perfil de exposure probado, no saltar directo desde pricing actual a valoracion de credito.

## Politica de errores

La API debe devolver errores estables y claros.

Reglas actuales:

- Inputs invalidos devuelven `400 Bad Request`.
- Campos faltantes devuelven `ApiError` con `details`.
- Enum invalido devuelve mensaje limpio, sin nombres internos de Jackson.
- Errores inesperados no deben filtrar stack traces ni clases internas.
- Resultados numericos `NaN` o `Infinity` se rechazan.

Esto es importante porque el frontend y futuros consumidores necesitan contratos previsibles.

## Como testeamos esta logica

El slice de pricing se protege con:

- Known-value tests para call y put.
- Put-call parity.
- Monotonicidad de call respecto a spot.
- Monotonicidad de call respecto a volatilidad.
- Tasa negativa valida.
- Rechazo de inputs invalidos.
- Rechazo de resultados no finitos.
- API tests con tolerancias numericas.
- API tests de error shape.

El slice de portfolio se protege con:

- Tests de dominio para nombres, simbolos, strikes y quantities.
- Integration tests con PostgreSQL/Testcontainers.
- Tests API para crear/listar/actualizar/borrar portfolios.
- Tests API para crear/listar/obtener/actualizar/borrar posiciones.
- Tests para validacion Blemberg habilitada y deshabilitada.
- Tests para la watchlist local temporal.
- Tests de errores `400` y `404` con `ApiError`.

El slice de exposure se protege con:

- Tests deterministas de GBM con seeds fijos.
- Tests de drift donde `dividendYield` cambia los paths simulados.
- Tests fixture de agregacion para EE, ENE y PFE.
- Tests de aplicacion para portfolios vacios, posiciones vencidas, regla USD-only y market data faltante.
- Tests API para requests validos e invalidos de simulacion.

La regla es:

> Si una formula financiera o un workflow persistido cambia, los tests deben decirnos si cambio por una razon explicita o si rompimos una propiedad esperada.

## Que logramos con portfolio

Portfolio ya tiene una primera version persistida y una primera valoracion stateless con Black-Scholes.

Esto nos da:

- Una unidad de negocio persistida.
- Una forma clara de guardar posiciones de opciones europeas.
- Pricing a nivel portfolio para posiciones europeas en USD.
- Perfiles de exposure para portfolios de opciones europeas en USD.
- Base para CVA mas adelante.

El orden que seguimos fue:

1. Pricing de una opcion individual.
2. Crear portfolios.
3. Agregar opciones a portfolios.
4. Recuperar portfolios.
5. Preparar pricing a nivel portfolio.
6. Valorar portfolios con Black-Scholes usando inputs desde `marketdata`.
7. Simular perfiles de exposure usando GBM y repricing Black-Scholes.

Asi evitamos construir Monte Carlo o CVA antes de tener una unidad persistida sobre la cual calcular riesgo.
Ahora Exposure V1 existe, y CVA debe esperar hasta que el contrato de exposure este estable.

## Proximo milestone recomendado

El siguiente milestone recomendado es endurecer el smoke real con Blemberg y preparar CVA simplificado.

Siguiente version sugerida:

- Mantener los endpoints actuales de portfolio pricing y exposure.
- Usar Blemberg real como fuente de `spot`, `volatility`, `riskFreeRate` y `dividendYield` cuando este corriendo.
- Mantener NexusXVA sin persistir market data.
- Agregar FX solo cuando queramos soportar totales multi-currency.
- Agregar CVA solo despues de que Exposure V1 tenga tests y response shape estables.

Fuera de scope inicial:

- CVA.
- Auth.
- Multi-currency sin FX.
- Netting/collateral.

## Como mantener este documento

Cada vez que agreguemos un slice importante, actualizar:

- Que flujo nuevo existe.
- Que modulos participan.
- Que supuestos financieros tomamos.
- Que queda fuera de scope.
- Que tests protegen la logica.
- Que milestone queda naturalmente despues.

Este archivo debe mantenerse corto, practico y orientado a decisiones.
