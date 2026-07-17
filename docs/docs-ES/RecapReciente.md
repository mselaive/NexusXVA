# Recap reciente de NexusXVA

Este recap resume los ultimos slices funcionales y tecnicos implementados en NexusXVA.
La idea es que cualquier dev pueda entender rapidamente donde esta parada la aplicacion antes de planear el siguiente milestone.

## Forma actual del producto

NexusXVA ya no es solo una API de pricing. Ahora es una pequena workstation de riesgo con flujos separados por rol:

- **FO** analiza portfolios, prepara trades, corre vistas de riesgo/pricing y monitorea P&L.
- **BO** valida trades, controla lifecycle, ejecuta EOD y revisa reporting operativo.
- **ADMIN** administra usuarios, permisos, horarios operativos, setup XVA, audit logs y workflows.

El flujo end-to-end actual es:

```text
FO analiza / bookea
  -> BO valida
  -> posiciones activas confirmadas
  -> pricing / exposure / CVA / stress / delta hedge
  -> EOD snapshots y P&L
  -> reporting, run history y audit trail
```

## Cambios recientes principales

### Workstation FO

FO ahora tiene un flujo diario mas completo:

- **FO Desk** como cockpit principal.
- **Pre-Trade Analysis** para impacto de trades hipoteticos antes de bookear.
- **Stress Testing** con matrices de escenarios.
- **u-Pad** para enviar bookings a BO.
- **Delta Hedge** para analisis de hedge entre opciones y cash equities.
- Pantallas de **Pricing**, **Exposure** y **CVA**.
- **Run History** y **Report History** para revisar corridas y vistas anteriores.

Limite importante:

- Pre-trade/stress analysis no bookean trades.
- u-Pad crea solicitudes pendientes.
- Pricing, exposure, CVA, stress y delta hedge usan solo posiciones confirmadas activas.

### Maker-checker de trade lifecycle

Los workflows de trade ahora pasan por BO:

- Nuevos trades se envian como booking requests.
- BO aprueba o rechaza bookings.
- Bookings aprobados crean posiciones confirmadas.
- FO puede pedir amendments o cancellations.
- BO aprueba/rechaza solicitudes de lifecycle.
- Amend aprobado marca la posicion original como `AMENDED` y crea una nueva `ACTIVE`.
- Cancel aprobado marca la posicion como `CANCELLED`.

Analytics usa solo posiciones `ACTIVE`.
Las posiciones historicas quedan visibles para auditoria y tracking de lifecycle.

### Cash equities y lots

Se agregaron cash equities junto a opciones europeas:

- FO puede bookear cash equities.
- BO approval crea posiciones confirmadas de cash equity.
- Se persisten execution lots.
- Ahora existen average cost, cost basis, unrealized P&L y realized P&L.
- Reducir una posicion de cash equity via amendment puede generar realized P&L.

Todavia no es un ledger contable completo, pero deja una base mucho mas fuerte para economics y P&L.

### EOD y P&L

EOD ahora se trata como proceso de control auditado:

- BO puede correr EOD global sobre portfolios activos.
- EOD crea snapshots inmutables de cierre por portfolio/posicion.
- Daily P&L usa el ultimo cierre EOD activo.
- Since-trade P&L usa economics de ejecucion.
- Cierres incorrectos se corrigen con `VOIDED` o `SUPERSEDED`, no borrado fisico.
- Recapture crea un nuevo cierre activo para la misma business date.
- Portfolios archivados no participan en nuevas operaciones, pero conservan historial.

EOD tambien puede programarse desde ADMIN Operational Control.

### Reporting FO/BO

El reporting operativo mejoro:

- FO P&L Snapshot.
- BO Operations Reporting.
- BO Lifecycle Reporting.
- Report History guarda vistas generadas en `report_snapshots`.

Report History responde: "que vio el usuario en ese momento?".
No reemplaza valuation runs, EOD, accounting ni recalculo.

### Valuation Run History

Pricing, Exposure y CVA ahora guardan snapshots auditados:

- input JSON.
- result JSON.
- summary JSON.
- usuario, grupo activo y scope.
- contexto de portfolio o netting set.
- estado `SUCCESS` o `FAILED`.

Run History es contexto de auditoria/revision.
No es EOD oficial, accounting ni market data.

### XVA Setup, Netting y Collateral

ADMIN ahora controla reference data XVA:

- counterparties.
- netting sets.
- asignaciones de portfolios.
- estado active/inactive.
- collateral estatico.
- credit curves.
- discount curves.

FO puede correr CVA sobre:

- un portfolio individual.
- un netting set activo.

Netting-set CVA V1 es solo a nivel perfil:

- agrega exposures de portfolios asignados.
- resta collateral estatico de buckets positivos.
- aplica la formula CVA simplificada.

Todavia no modela legal netting path-level, CSA margining, margin calls, wrong-way risk ni DVA/FVA/KVA.

### Lifecycle de curvas

Las curvas persistidas de CVA ahora tienen lifecycle y versionado:

```text
ADMIN crea curva
  -> version DRAFT
  -> ADMIN aprueba o rechaza
  -> la curva APPROVED queda activa
  -> la version activa anterior queda SUPERSEDED
  -> CVA solo puede referenciar curvas activas APPROVED
```

Reglas:

- Curvas nuevas nacen como `DRAFT`.
- Curvas draft pueden editarse.
- Curvas aprobadas/rechazadas/superseded quedan como historial inmutable.
- Aprobar una nueva version supersede la version activa aprobada anterior.
- CVA solo puede usar curvas persistidas `APPROVED` y activas.
- `source` soporta `MANUAL`, `IMPORT` y `MARKET_DATA`.
- Imports reales y curvas sourced desde Blemberg quedan como trabajo futuro.

### Operational Control

ADMIN puede configurar un calendario operativo global:

- timezone.
- business days.
- trading open/close.
- schedule de EOD.
- politica de market data stale para EOD.
- bloqueos separados para:
  - bookings FO y solicitudes FO de lifecycle.
  - risk runs como pricing, pre-trade, stress, delta hedge, exposure y CVA.

El backend es la autoridad.
Botones deshabilitados en frontend son ayuda visual, no frontera de seguridad.

### Audit Trail y technical logs

NexusXVA separa:

- **Audit Trail** en PostgreSQL para actividad de usuarios y control operativo.
- **Technical logs** en archivos rotados para debugging backend.

Audit events incluyen login, cambio de grupo, accesos denegados, bookings, approvals, lifecycle decisions, correcciones EOD, cambios de setup y requests de valuacion.

Technical logs incluyen system, auth, market-data integration, EOD jobs y errors, con correlation ids.

### Integracion Blemberg

NexusXVA usa Blemberg a traves de la frontera `marketdata`:

- validacion de instrumentos.
- pricing inputs.
- snapshots/coverage diagnosticos en frontend.
- refresh prioritario via endpoints proxy de NexusXVA.

NexusXVA sigue sin persistir market data como source of truth.
Blemberg es dueno del market data cacheado; NexusXVA es dueno de portfolios, workflows, valuation y audit.

## Ownership actual de datos

Ownership de alto nivel:

- **Portfolio module**: portfolios, posiciones confirmadas y terminos del trade.
- **Trade booking/lifecycle modules**: estado pending/approved/rejected de workflows.
- **Portfolio/EOD modules**: snapshots de cierre y referencias de P&L.
- **XVA module**: counterparties, netting sets, collateral y curvas.
- **Valuation runs**: snapshots auditados de calculos.
- **Report snapshots**: vistas guardadas de workstation.
- **Audit module**: historial de actividad de usuario.
- **Blemberg**: market data.

## Limites importantes

NexusXVA intencionalmente no:

- persiste market data de Blemberg como source of truth.
- usa curvas draft para CVA.
- usa bookings pendientes en pricing/exposure/CVA.
- borra fisicamente cierres EOD.
- borra fisicamente historial operativo.
- trata Report History como fuente de recalculo.
- trata Run History como accounting oficial.
- implementa ledger contable completo todavia.

## Lo que se siente estable ahora

Las areas mas fuertes son:

- workflows FO/BO/ADMIN por rol.
- pricing de opciones europeas y portfolio pricing.
- Exposure V1 y CVA simplificado.
- BO trade validation y lifecycle.
- modelo de correccion EOD.
- XVA setup con netting-set CVA.
- versionado/aprobacion de curvas.
- fundaciones de run/report/audit history.

## Siguientes pasos naturales

Trabajo recomendado:

1. **Imports de curvas y curvas sourced desde market data**
   - upload CSV/manual.
   - curvas discount/risk-free desde Blemberg.
   - preview de validacion antes de approve.

2. **Accounting mas completo para cash equities**
   - buy/sell lots.
   - partial closes.
   - realized P&L en reducciones.
   - discutir politica average-cost vs FIFO/LIFO.

3. **Mejores exports de reporting**
   - CSV export para reportes BO.
   - PDF o printable run summaries.
   - filtros guardados.

4. **Hardening de counterparty/CVA**
   - perfiles de collateral.
   - asignacion de credit curves por counterparty.
   - defaults de curvas por netting set.

5. **Market risk e historicos**
   - cuando Blemberg tenga cobertura historica madura.
   - VaR/stress desde retornos historicos.
   - evitar duplicar market data en NexusXVA salvo que exista un caso claro de derived data.

