# Lab 06 - Estabilización de módulo de incidencias y mantenimiento de equipos para una red de laboratorios clínicos

## Contexto

Has ingresado como backend developer a **AndeLab Perú**, una red de laboratorios clínicos con varias sedes en el país.

La empresa ya cuenta con un backend desarrollado para gestionar:

- sedes
- equipos de laboratorio
- incidencias
- órdenes de trabajo
- repuestos

Este sistema ya se encuentra levantado y es usado de forma interna por distintas áreas. Sin embargo, el módulo presenta fallas funcionales, reglas de negocio mal aplicadas, comportamientos inconsistentes y algunos problemas de seguridad que ya fueron reportados por usuarios internos y por QA.

Tu trabajo consiste en:

- entender el flujo del negocio
- revisar el código actual
- reproducir problemas reales
- corregir errores
- reforzar reglas de negocio
- estabilizar el comportamiento general de la API
- dejar el sistema en mejor estado técnico

Este laboratorio busca que trabajes sobre una base existente, con criterio de mantenimiento evolutivo y correctivo.

---

## Reglas de negocio esperadas

El sistema debe respetar como mínimo estas reglas.

### Sedes

- una sede puede estar en estado `ACTIVA` o `INACTIVA`
- no se debe registrar operación logística o de incidencias vinculada a sedes inactivas cuando la regla del módulo lo prohíba

### Equipos de laboratorio

- un equipo puede estar en estados como `OPERATIVO`, `EN_MANTENIMIENTO`, `FUERA_SERVICIO` o `DE_BAJA`
- no se debe permitir registrar incidencias para equipos:
  - `DE_BAJA`
  - `FUERA_SERVICIO`
  - o con `activo = false`

### Incidencias

- una incidencia inicia en estado `REGISTRADA`
- una incidencia solo puede pasar a `EN_REVISION` desde `REGISTRADA`
- una incidencia solo puede aprobarse o rechazarse desde `EN_REVISION`
- una incidencia solo puede cerrarse desde `RESUELTA`
- no debe permitirse crear incidencias activas duplicadas para el mismo equipo y el mismo tipo
- la `fechaLimiteAtencion` debe calcularse automáticamente según prioridad:
  - `CRITICA`: +4 horas
  - `ALTA`: +8 horas
  - `MEDIA`: +24 horas
  - `BAJA`: +48 horas

### Órdenes de trabajo

- una orden de trabajo solo puede crearse desde una incidencia en estado `APROBADA`
- al crear la orden, la incidencia debe pasar a `EN_ATENCION`
- una orden puede asignarse solo desde `CREADA`
- una orden puede iniciarse solo desde `ASIGNADA`
- una orden puede finalizarse solo desde `EN_PROCESO`
- una orden ya finalizada no debe volver a procesarse como si fuera una finalización nueva

### Repuestos y stock

- al finalizar una orden, si se consumen repuestos, el descuento debe ser consistente
- el stock no debe quedar en estado parcial si una parte de la operación falla
- si existe conflicto concurrente de stock, la API debe responder con error controlado
- un repuesto está en stock bajo cuando `stockActual <= stockMinimo`

### Seguridad y acceso

- `SEDE` solo debe ver incidencias y órdenes asociadas a su propia sede
- `OPERACIONES` puede revisar incidencias y gestionar la operación general
- `TECNICO` solo debe ver órdenes asignadas a él y operar sobre las que le correspondan
- `ADMIN` tiene acceso total

### Contrato de errores

- la API debe responder errores en formato uniforme
- las respuestas de error deben seguir una estructura consistente como:
  ```json
  {
    "code": "SOME_CODE",
    "message": "Mensaje humano"
  }
  ```

---

## Síntomas reportados por el negocio y por QA

Las áreas usuarias y el equipo de QA reportaron comportamientos anómalos en el sistema. No asumas que esta lista cubre todo, pero sí representa los principales problemas observados.

### Reportes funcionales

- algunas incidencias parecen duplicarse aunque ya existe una abierta para el mismo equipo
- ciertas incidencias urgentes muestran fechas límite que no parecen coherentes con la prioridad
- se han registrado incidencias sobre equipos que no deberían aceptar nuevas atenciones
- en algunos casos se han creado órdenes de trabajo desde incidencias que aparentemente aún no estaban listas para eso
- se ha observado que ciertas órdenes cambian de estado cuando no deberían
- algunas finalizaciones de órdenes generan comportamientos extraños en el stock de repuestos
- se sospecha que repetir ciertas operaciones puede volver a descontar stock

### Reportes de seguridad y acceso

- existe sospecha de que un usuario con rol de sede puede consultar información que no le corresponde si accede directamente por identificador
- algunas restricciones por rol no parecen estar completamente blindadas en todos los escenarios

### Reportes técnicos

- la API no siempre responde errores con la misma estructura
- algunos endpoints parecen devolver respuestas distintas al contrato esperado
- ciertos casos borde de stock bajo no coinciden con lo que espera negocio
- el sistema funciona en términos generales, pero presenta inconsistencias que afectan la confiabilidad del módulo

Tu trabajo consiste en validar estos reportes, encontrar la causa real y corregirlos de manera consistente.

---

## Ruta sugerida de trabajo

No empieces modificando código al azar.

Se recomienda seguir esta ruta para trabajar con método y evitar que el laboratorio se convierta en una búsqueda a ciegas.

### Paso 1. Levanta el sistema y entiende el contexto

Antes de corregir nada:

- revisa las entidades principales
- identifica roles del sistema
- revisa los endpoints expuestos
- entiende el flujo esperado de:
  - incidencia
  - revisión
  - creación de orden
  - asignación
  - inicio
  - finalización
  - cierre

### Paso 2. Reproduce casos desde la API

Usa Swagger o tu cliente HTTP favorito para recorrer el flujo real.

Empieza por:

- autenticación
- consulta de sedes
- consulta de equipos
- creación de incidencia
- cambio de estados de incidencia
- creación de orden
- transición de orden
- finalización con repuestos
- consulta de repuestos y stock bajo

### Paso 3. Contrasta comportamiento vs regla de negocio

Cada vez que encuentres un comportamiento extraño, compáralo contra las reglas esperadas del negocio.

No corrijas solo porque “se ve raro”.  
Corrige porque puedes demostrar que el comportamiento actual contradice una regla esperada.

### Paso 4. Prioriza lo más crítico

Corrige primero lo que tenga mayor impacto:

1. seguridad por rol y acceso indebido
2. validaciones de negocio
3. transiciones de estado
4. consistencia de stock y finalización
5. contrato uniforme de errores
6. mejoras de arquitectura o limpieza

### Paso 5. Refactoriza con criterio

Si detectas lógica de negocio mal ubicada, duplicaciones o respuestas inconsistentes, mejora esa parte del diseño sin reescribir innecesariamente el proyecto entero.

### Paso 6. Verifica después de cada corrección

No acumules muchos cambios sin validar.

Después de cada corrección importante:

- reproduce el caso que fallaba
- valida que ahora responda correctamente
- comprueba que no rompiste un flujo relacionado

---

## Entregables

Debes entregar lo siguiente.

### 1. Código corregido

Tu solución debe dejar el proyecto en un estado más estable y consistente que el recibido.

### 2. Archivo `HALLAZGOS.md`

Debes documentar de forma clara los problemas encontrados.

Cada hallazgo debe incluir como mínimo:

- nombre breve del problema
- síntoma observado
- regla de negocio afectada
- causa encontrada
- solución aplicada

### 3. Archivo `DECISIONES.md`

Debes documentar decisiones técnicas relevantes tomadas durante la corrección.

Incluye, por ejemplo:

- refactors aplicados
- por qué moviste cierta lógica
- cómo mejoraste manejo de errores
- qué trade-offs encontraste

### 4. Evidencia de validación

Debes incluir evidencia de que probaste lo corregido.

Puede ser mediante una de estas opciones:

- pruebas automáticas
- colección de requests
- capturas o notas claras de pruebas manuales

No basta con decir “ya funciona”.  
Debes dejar trazabilidad de cómo validaste.

---

## Checklist mínimo

Tu entrega debe cumplir como mínimo con lo siguiente:

- el proyecto levanta correctamente
- la autenticación sigue funcionando
- Swagger sigue funcionando
- ya no se permiten incidencias activas duplicadas para el mismo equipo y tipo
- la fecha límite de atención se calcula correctamente para todas las prioridades
- ya no se puede crear incidencia para equipos no permitidos
- un usuario con rol `SEDE` no puede consultar incidencias u órdenes de otra sede
- ya no se puede crear una orden desde una incidencia en estado inválido
- las transiciones de estado de órdenes están correctamente restringidas
- la finalización de órdenes no deja stock inconsistente por fallos parciales
- el sistema maneja correctamente conflictos de stock en vez de responder con error genérico inesperado
- una doble finalización no vuelve a descontar stock
- el endpoint de stock bajo devuelve correctamente los repuestos cuyo stock es menor o igual al mínimo
- la API responde errores con formato uniforme en los casos corregidos
- los endpoints públicos del laboratorio respetan un contrato de respuesta consistente
- entregaste `HALLAZGOS.md`
- entregaste `DECISIONES.md`
- dejaste evidencia de validación de los casos corregidos

---

## Nota final

Este laboratorio no evalúa únicamente si encontraste errores.

También evalúa si trabajaste:

- entendiendo antes de tocar
- reproduciendo antes de corregir
- justificando cada cambio con una regla o un impacto real
- manteniendo el sistema coherente
- dejando evidencia clara de tu trabajo
