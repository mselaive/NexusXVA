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
4. Monte Carlo simulation.
5. Exposure analytics.
6. CVA simplificado.
7. Dashboard.

La idea es que cada paso use lo anterior, sin adelantarse demasiado.

## Estado actual

Actualmente el backend tiene:

- Health endpoint.
- Manejo de errores API estable.
- Estructura modular por paquetes.
- Pricing stateless de opciones europeas con Black-Scholes.
- Calculo de Greeks principales.
- Portfolio management persistido en PostgreSQL.
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
  -> portfolio.infrastructure
  -> PostgreSQL
  -> HTTP response
```

En mas detalle:

1. El controller recibe JSON.
2. El DTO valida campos obligatorios y rangos basicos.
3. El DTO se transforma a un comando de aplicacion.
4. El servicio de aplicacion abre la transaccion y ejecuta el caso de uso.
5. El puerto `PortfolioStore` abstrae la persistencia.
6. El adaptador JPA persiste o recupera entidades.
7. La API devuelve DTOs, no entidades JPA.

Esto permite tener persistencia real sin mezclar reglas HTTP, reglas de dominio y detalles de base de datos.

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

## Decisiones financieras actuales

Para el primer pricing slice decidimos:

- Soportar solo opciones europeas `CALL` y `PUT`.
- Usar Black-Scholes clasico.
- Usar volatilidad constante.
- Usar tasa libre de riesgo constante y continuamente compuesta.
- No incluir dividend yield todavia.
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
- Guardar `underlyingSymbol`, `optionType`, `strike`, `maturityDate` y `quantity`.
- Permitir quantity positiva o negativa para representar posiciones long/short.
- Rechazar quantity igual a cero.
- Permitir `maturityDate` en el pasado; pricing decidira si puede valorar trades vencidos.
- No guardar `spot`, `volatility` ni `riskFreeRate` dentro de la posicion.

La separacion trade/market data es intencional.
Un portfolio describe que instrumentos tenemos; market data describe el estado del mercado usado para valorar esos instrumentos.
Por eso el siguiente pricing a nivel portfolio deberia leer posiciones persistidas y recibir market data en el request o desde un modulo dedicado.

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
- Tests de errores `400` y `404` con `ApiError`.

La regla es:

> Si una formula financiera o un workflow persistido cambia, los tests deben decirnos si cambio por una razon explicita o si rompimos una propiedad esperada.

## Que logramos con portfolio

Portfolio ya tiene una primera version persistida.

Esto nos da:

- Una unidad de negocio persistida.
- Una forma clara de guardar posiciones de opciones europeas.
- Base para pricing a nivel portfolio.
- Base para simulacion, exposure y CVA mas adelante.

El orden que seguimos fue:

1. Pricing de una opcion individual.
2. Crear portfolios.
3. Agregar opciones a portfolios.
4. Recuperar portfolios.
5. Preparar pricing a nivel portfolio.

Asi evitamos construir Monte Carlo o CVA antes de tener una unidad persistida sobre la cual calcular riesgo.

## Proximo milestone recomendado

El siguiente milestone recomendado es portfolio-level pricing.

Primera version sugerida:

- Recibir un portfolio id.
- Leer posiciones persistidas.
- Recibir market data en el request.
- Convertir `maturityDate` a `timeToMaturityYears`.
- Reutilizar el calculator Black-Scholes existente.
- Devolver precio y Greeks por posicion y un total de portfolio.

Fuera de scope inicial:

- Monte Carlo.
- Exposure.
- CVA.
- Auth.
- Multi-currency.
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
