---
okf: "0.1"
id: "CP-1.1.1"
titulo: "Casos de prueba - API Buggy Cars Rating"
estado: ejecutado
fecha_ejecucion: 2026-11-11
automatizacion: "src/test/java/api/buggycars/buggy-cars.feature"
reporte: "target/karate-reports/karate-summary.html"
---

# Casos de prueba - API Buggy Cars Rating

**URL base:** `https://k51qryqov3.execute-api.ap-southeast-2.amazonaws.com/prod`
**Precondición general:** la API responde y existe el usuario `qa_tmp_x` / `Abcdef1!` (nombre Qa Test, sin votos registrados).
**Datos comunes:** Lamborghini `ckl2phsabijs71623vk0`, Pagani `ckl2phsabijs7162403g`, Diablo `ckl2phsabijs71623vk0|ckl2phsabijs71623vqg`.

## Resumen de ejecución (2026-11-11)

| Total casos | Iteraciones | Aprobados | Fallidos | % éxito |
|---|---|---|---|---|
| 20 | 23 | 19 | 1 | 95 % |

| ID | Título | Tipo | Resultado |
|---|---|---|---|
| CP-01 | Consultar Lamborghini, página 1 de modelos | Positivo | ✅ Aprobado |
| CP-02 | Consultar Lamborghini, página 2 de modelos | Positivo | ✅ Aprobado |
| CP-03 | Página de modelos fuera de rango | Borde | ✅ Aprobado |
| CP-04 | Consultar marca Pagani | Positivo | ✅ Aprobado |
| CP-05 | Marca inexistente | Negativo | ✅ Aprobado |
| CP-06 | Detalle del modelo Diablo sin autenticación | Positivo | ✅ Aprobado |
| CP-07 | Modelo inexistente | Negativo | ✅ Aprobado |
| CP-08 | Ranking ordenado por posición | Positivo | ✅ Aprobado |
| CP-09 | Ranking ordenado por marca | Positivo | ✅ Aprobado |
| CP-10 | Dashboard | Positivo | ✅ Aprobado |
| CP-11 | Login válido | Positivo | ✅ Aprobado |
| CP-12 | Login inválido | Negativo | ✅ Aprobado |
| CP-13 | Usuario actual con token válido | Positivo | ✅ Aprobado |
| CP-14 | Usuario actual sin token | Negativo | ✅ Aprobado |
| CP-15 | Usuario actual con token inválido | Negativo | ✅ Aprobado |
| CP-16 | Perfil del usuario autenticado | Positivo | ✅ Aprobado |
| CP-17 | Usuario autenticado puede votar | Positivo | ✅ Aprobado |
| CP-18 | Votar sin autenticación | Negativo | ✅ Aprobado |
| CP-19 | Validaciones de registro (4 iteraciones) | Negativo | ✅ Aprobado |
| CP-20 | Registro con contraseñas distintas | Negativo | ❌ Fallido (defecto) |

## Defecto encontrado

**DEF-01 - El registro acepta contraseña y confirmación distintas**
- **Severidad:** media. **Caso:** CP-20.
- **Pasos:** `POST /users` con `{"username":"qa_tmp_mismatch","firstName":"Qa","lastName":"Test","password":"Abcdef1!","confirmPassword":"Xbcdef1!"}`.
- **Esperado:** 400 con un mensaje de validación. **Obtenido:** 201 `{}` y el usuario se crea.
- **Nota:** en ejecuciones posteriores la API responde 400 `User already exists`. El caso sigue fallando a propósito porque ese mensaje no corresponde a la validación esperada.

---

## Detalle de casos

### CP-01 - Consultar Lamborghini, página 1 de modelos
- **Precondiciones:** ninguna.
- **Pasos:** 1. Enviar `GET /makes/ckl2phsabijs71623vk0?modelsPage=1`.
- **Resultado esperado:** 200; `name` = "Lamborghini"; `description`, `image` texto; `models.totalPages` numérico; lista de modelos no vacía, cada uno con `id, name, image, make, makeId, makeImage, votes, rank, engineVol, comments, totalComments` y `make` = "Lamborghini".
- **Datos:** makeId Lamborghini.

### CP-02 - Consultar Lamborghini, página 2 de modelos
- **Pasos:** `GET /makes/ckl2phsabijs71623vk0?modelsPage=2`.
- **Resultado esperado:** 200; lista de modelos no vacía y todos con `make` = "Lamborghini".

### CP-03 - Página de modelos fuera de rango
- **Pasos:** `GET /makes/ckl2phsabijs71623vk0?modelsPage=99`.
- **Resultado esperado:** 200; `name` = "Lamborghini"; `models.models` = `[]`; `totalPages` numérico.

### CP-04 - Consultar marca Pagani
- **Pasos:** `GET /makes/ckl2phsabijs7162403g?modelsPage=1`.
- **Resultado esperado:** 200; `name` = "Pagani"; todos los modelos con `make` = "Pagani" y `makeId` de Pagani.

### CP-05 - Marca inexistente
- **Pasos:** `GET /makes/nonexistent?modelsPage=1`.
- **Resultado esperado:** 404 `{"message":"Not Found"}`.

### CP-06 - Detalle del modelo Diablo sin autenticación
- **Pasos:** `GET /models/ckl2phsabijs71623vk0|ckl2phsabijs71623vqg` sin cabecera Authorization.
- **Resultado esperado:** 200; `name` = "Diablo"; `description` texto; `votes`, `engineVol` numéricos; cada comentario con `user`, `datePosted`, `text`; `canVote` = false.

### CP-07 - Modelo inexistente
- **Pasos:** `GET /models/ckl2phsabijs71623vk0|noexiste`.
- **Resultado esperado:** 404 `{"message":"Not Found"}`.

### CP-08 - Ranking ordenado por posición
- **Pasos:** `GET /models?page=1&orderBy=rank`.
- **Resultado esperado:** 200; `totalPages` numérico; modelos con el esquema completo; valores `rank` en orden ascendente.

### CP-09 - Ranking ordenado por marca
- **Pasos:** `GET /models?page=1&orderBy=make`.
- **Resultado esperado:** 200; campo `make` en orden alfabético.

### CP-10 - Dashboard
- **Pasos:** `GET /dashboard`.
- **Resultado esperado:** 200; `make` = `{id, name, image, votes}`; `model` = `{id, make, name, image, votes}`; `model.id` con formato `makeId|modelId`.

### CP-11 - Login válido
- **Pasos:** `POST /oauth/token` (form) `grant_type=password`, `username=qa_tmp_x`, `password=Abcdef1!`.
- **Resultado esperado:** 200; `access_token` y `refresh_token` texto; `token_type` = "Bearer"; `expires_in` = 3600.

### CP-12 - Login inválido
- **Pasos:** `POST /oauth/token` con `username=usuario_inexistente_qa`, `password=Clave123!`.
- **Resultado esperado:** 401 con cuerpo "Invalid credentials".

### CP-13 - Usuario actual con token válido
- **Precondiciones:** token obtenido con el login de CP-11.
- **Pasos:** `GET /users/current` con `Authorization: Bearer <token>`.
- **Resultado esperado:** 200 `{"firstName":"Qa","lastName":"Test","isAdmin":false}`.

### CP-14 - Usuario actual sin token
- **Pasos:** `GET /users/current` sin cabecera.
- **Resultado esperado:** 401 `{"message":"Not authorized."}`.

### CP-15 - Usuario actual con token inválido
- **Pasos:** `GET /users/current` con `Authorization: Bearer token-invalido`.
- **Resultado esperado:** 401 `{"message":"Not authorized."}`.

### CP-16 - Perfil del usuario autenticado
- **Precondiciones:** token válido.
- **Pasos:** `GET /users/profile` con token.
- **Resultado esperado:** 200; `username` = "qa_tmp_x", `firstName` = "Qa", `lastName` = "Test"; `gender, age, address, phone, hobby` texto.

### CP-17 - Usuario autenticado puede votar
- **Precondiciones:** token válido de un usuario que no ha votado por Diablo.
- **Pasos:** `GET /models/{Diablo}` con token.
- **Resultado esperado:** 200; `canVote` = true.

### CP-18 - Votar sin autenticación
- **Pasos:** `POST /models/{Diablo}/vote` con `{"comment":"Voto sin autenticacion"}` sin token.
- **Resultado esperado:** 401 `{"message":"Not authorized."}`; no se registra ningún voto.

### CP-19 - Validaciones de registro
- **Pasos:** `POST /users` con cada cuerpo:

| Iteración | Cuerpo | Mensaje esperado (400) |
|---|---|---|
| Sin username | `{}` | `username is required` |
| Sin password | username, firstName, lastName | `password is required` |
| Password corta | password `abc` | `minimum field size of 6` |
| Username duplicado | username `qa_tmp_x` | `User already exists` |

### CP-20 - Registro con contraseñas distintas
- **Pasos:** `POST /users` con `password` = `Abcdef1!` y `confirmPassword` = `Xbcdef1!`.
- **Resultado esperado:** 400 con mensaje de validación (y no `User already exists`).
- **Resultado obtenido:** 201 → **DEF-01**.
