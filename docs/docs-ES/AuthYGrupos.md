# Auth y Grupos

NexusXVA Auth V1 agrega login con usuarios, grupos y sesiones persistidas.
El objetivo de este slice es autenticar bien y dejar preparada la autorizacion por grupos.

## Grupos iniciales

Los grupos se guardan en `auth_groups`.
Los grupos iniciales son:

- `FO`: Front Office. Usuarios que bookean trades y corren analitica de pricing/riesgo.
- `BO`: Back Office. Usuarios que revisan posiciones, lifecycle y datos operativos.
- `ADMIN`: Administradores. Usuarios que administran membresias, permisos FO, visibilidad de portfolios y monitoreo de workflow.

Un usuario puede pertenecer a mas de un grupo.
La relacion usuario-grupo se guarda en `auth_user_group_memberships`.

## Tablas

- `auth_user_accounts`: usuarios.
- `auth_groups`: grupos.
- `auth_user_group_memberships`: relacion N:M entre usuarios y grupos.
- `auth_sessions`: sesiones activas/revocadas.

## Passwords

Las passwords nunca se guardan en claro.
Se guardan como hash BCrypt en `auth_user_accounts.password_hash`.

El backend no debe loguear passwords ni devolverlas por API.

## Sesiones

El login crea una sesion opaca:

- El browser recibe una cookie `HttpOnly` llamada `NEXUSXVA_SESSION`.
- La base de datos guarda solamente el hash SHA-256 del token de sesion.
- Si alguien lee la tabla `auth_sessions`, no obtiene el token raw que sirve para autenticarse.

La sesion tambien tiene:

- `created_at`
- `expires_at`
- `revoked_at`
- `active_group_code`

## Grupo activo

Un usuario puede pertenecer a varios grupos, pero cada sesion trabaja con un solo grupo activo.

Despues del login, el frontend llama:

```text
POST /api/auth/active-group
```

El backend verifica que el usuario pertenezca al grupo solicitado y guarda la eleccion en `auth_sessions`. La autorizacion no depende de `localStorage`.

- `FO`: FO Desk, Pre-Trade Analysis, Stress Testing, u-Pad, portfolios, pricing, exposure y CVA.
- `BO`: Trade Validation y Trading Limits preventivos para usuarios FO.
- `ADMIN`: administracion de usuarios, grupos, permisos FO, visibilidad de portfolios y mapa de workflow.

## Administracion V1

ADMIN no bookea ni aprueba trades. Su responsabilidad es controlar acceso y observar el flujo.

Endpoints principales:

- `GET /api/admin/users`
- `GET /api/admin/users/{userId}`
- `PUT /api/admin/users/{userId}/groups`
- `PUT /api/admin/users/{userId}/permissions`
- `PUT /api/admin/users/{userId}/portfolio-access`
- `GET /api/admin/portfolios`
- `GET /api/admin/workflow-map`

El modelo queda en dos capas:

- Grupo activo: decide si el usuario entra como `FO`, `BO` o `ADMIN`.
- Permisos/visibilidad: refinan que puede hacer un usuario FO y que portfolios puede ver.

Permisos FO iniciales:

- `FO_BOOK_TRADES`: permite enviar bookings desde `u-Pad`.
- `FO_CREATE_PORTFOLIOS`: permite crear portfolios.
- `FO_RUN_CVA`: permite ejecutar CVA.
- `FO_RUN_WHAT_IF`: permite ejecutar Pre-Trade Analysis stateless.
- `FO_RUN_STRESS_TEST`: permite ejecutar Stress Testing stateless.

La visibilidad de portfolios puede ser `ALL` o `SELECTED`. Por defecto los usuarios existentes quedan con `ALL` y permisos habilitados, para no romper el flujo local hasta que ADMIN restrinja accesos.

El mapa de workflow es solo lectura. Muestra solicitudes en `Booked`, `Waiting BO`, `Accepted` y `Rejected`; la aprobacion o rechazo sigue perteneciendo a BO Trade Validation.

## CSRF

Como usamos cookie `HttpOnly`, las mutaciones autenticadas usan `X-CSRF-Token`.

El frontend obtiene ese token desde:

- `POST /api/auth/login`
- `GET /api/auth/me`

Luego lo manda en requests mutantes como `POST`, `PUT`, `PATCH` y `DELETE`.

## Bootstrap Admin

En Docker Compose, Auth V1 queda habilitado por default.

Usuario inicial de desarrollo:

- username: `admin`
- password: `admin12345`

Esto es solo para desarrollo local.
Antes de compartir un entorno, cambiar:

```bash
NEXUSXVA_BOOTSTRAP_ADMIN_USERNAME=admin
NEXUSXVA_BOOTSTRAP_ADMIN_PASSWORD=change-this-password
```

## Siguiente Slice

Auth valida la sesion, CSRF, el grupo activo y los primeros permisos administrativos.
El siguiente slice de seguridad natural sera crear usuarios desde ADMIN, auditoria mas rica y permisos adicionales por producto o workflow.
