# Auth y Grupos

NexusXVA Auth V1 agrega login con usuarios, grupos y sesiones persistidas.
El objetivo de este slice es autenticar bien y dejar preparada la autorizacion por grupos.

## Grupos iniciales

Los grupos se guardan en `auth_groups`.
Los grupos iniciales son:

- `FO`: Front Office. Usuarios que bookean trades y corren analitica de pricing/riesgo.
- `BO`: Back Office. Usuarios que revisan posiciones, lifecycle y datos operativos.
- `ADMIN`: Administradores. Usuarios que luego podran administrar usuarios, grupos y settings.

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

- `FO`: portfolios, u-Pad, pricing, exposure y CVA.
- `BO`: Trade Validation y Trading Limits preventivos para usuarios FO.
- `ADMIN`: reservado para administracion de usuarios y grupos.

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

Auth valida la sesion, CSRF y el grupo activo por endpoint.
El siguiente slice de seguridad sera administrar usuarios y memberships desde ADMIN.
