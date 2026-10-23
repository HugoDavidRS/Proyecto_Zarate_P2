# Procedimiento de desarrollo

Este documento cuenta cómo se construyó la **Calculadora Científica**, paso a paso: qué se buscaba en cada etapa, qué problemas aparecieron y cómo se resolvieron.
Cada etapa corresponde a una versión con su etiqueta en git, así que se puede revisar el código exacto de cualquier punto del proceso.

| Etapa | Versión | Commit / etiqueta |
|---|---|---|
| 1. Proyecto base | v0.1.0 | `git checkout v0.1.0` |
| 2. Primera calculadora | v1.0.0 | `git checkout v1.0.0` |
| 3. Experimento DOOM | v1.1.0 → v1.2.0 | `git checkout v1.1.0`, `git checkout v1.2.0` |
| 4. Retiro de DOOM y mejoras visuales | v1.3.0 → v1.3.1 | `git checkout v1.3.0` |
| 5. Herramientas científicas | v2.0.0 → v2.0.1 | `git checkout v2.0.0` |
| 6. Versión completa | v3.0.0 | `git checkout v3.0.0` |

---

## 1. Proyecto base

- Se creó el proyecto en Android Studio con la plantilla **Empty Activity** (Jetpack Compose, Kotlin).
- Configuración: `minSdk 24` (Android 7.0), `compileSdk`/`targetSdk 37`, Compose BOM 2026.02.01.
- El dispositivo de pruebas es un emulador con **Android 7.0**, así que todo se diseñó para funcionar en esa versión.

## 2. Primera calculadora (v1.0.0)

**Objetivo:** una calculadora científica completa y funcional.

Decisiones:

1. **Motor propio de expresiones** (`CalculatorEngine.kt`). Se eligió un analizador por **descenso recursivo** en lugar de evaluar texto con librerías externas:
   - permite controlar la precedencia (`^` antes que `×`, y `×` antes que `+`)
   - permite la multiplicación implícita (`2π`, `3(4+1)`)
   - permite los modos de ángulo DEG, RAD y GRAD
2. **Interfaz con Jetpack Compose** inspirada en la calculadora de iOS: fondo oscuro, botones redondeados y operaciones en naranja.
3. Para el botón `=` de doble altura se usó un `Row` con dos columnas (teclado y botón), porque una cuadrícula perezosa no permite celdas que abarquen varias filas.

Problemas resueltos:

- La función de combinaciones `nCr` tenía una fórmula incorrecta; se corrigió a `n! / (k! · (n−k)!)`.
- El display usaba a la vez `Arrangement.SpaceBetween` y un `Spacer` con peso, y se acomodaba mal; se dejó solo el `Spacer`.

## 3. Experimento DOOM

**Objetivo:** agregar el juego DOOM (código abierto) en un botón junto a `DEG`.

| Intento | Qué se hizo | Resultado | Causa |
|---|---|---|---|
| 1 (v1.1.0) | `WebView` con **js-dos** cargado desde `file://` | Pantalla girada y carga infinita | js-dos necesita WebAssembly y peticiones CORS que el WebView de Android 7.0 no permite desde archivos locales |
| 2 | Cargar el juego desde **archive.org** | La app se cerraba | Faltaba el permiso `ACCESS_NETWORK_STATE` para revisar la conexión |
| 3 | Agregar el permiso | Pantalla en blanco | archive.org bloquea que su página se muestre dentro de otras apps (`X-Frame-Options: SAMEORIGIN`) |
| 4 (v1.2.0) | **Motor raycaster propio** en un solo archivo HTML/JavaScript, sin internet | La pantalla de inicio funcionaba | El rendimiento del equipo de pruebas no era suficiente para una experiencia jugable |

**Decisión:** retirar DOOM y enfocarse en que la calculadora fuera la mejor posible (v1.3.0).

**Lección:** en Android 7.0 el `WebView` es antiguo, y las soluciones que dependen de WebAssembly o de páginas externas no son confiables.

## 4. Retiro de DOOM y mejoras visuales (v1.3.0 → v1.3.1)

- Se eliminó el botón DOOM y se regresó a la fila original: `DEG ( ) % ⌫`.
- Se añadieron:
  - temas de color
  - vibración al tocar (`LocalHapticFeedback`)
  - animación de pulsación (`animateFloatAsState` con resorte)
  - sacudida al haber un error (`Animatable` con `keyframes`)
  - copiar el resultado al portapapeles
- **Error encontrado:** el ícono de tema no respondía al tocarlo porque su `onClick` estaba vacío. Se corrigió con un botón "Tema" visible (v1.3.1), y se añadieron degradados y animaciones al display.

## 5. Herramientas científicas (v2.0.0 → v2.0.1)

Se agregaron siete herramientas, cada una en un panel de pantalla completa:

- constantes
- unidades
- estadística
- bases numéricas
- graficadora
- ecuaciones
- ajustes

La lógica matemática se separó en `MathTools.kt` (código puro, fácil de probar) y la interfaz en `ToolPanels.kt`.

Mejoras de uso:

- Pulsación larga para la segunda función de cada botón.
- Al deslizar sobre la pantalla se borra el último elemento.
- `⌫` borra funciones completas (`sin(`) en lugar de una letra.
- Después de `=`, un número empieza un cálculo nuevo y un operador continúa desde el resultado.
- El historial, el tema y el modo de ángulo se guardan con `SharedPreferences`; la expresión sobrevive a la rotación con `rememberSaveable`.

**Errores encontrados al revisar el código y corregidos:**

| Error | Causa | Solución |
|---|---|---|
| `√` siempre daba error | El símbolo `√` no se traducía a `sqrt` | Normalizar los símbolos antes de calcular |
| Fallaba el segundo cálculo con el teléfono en español | `String.format` usaba coma decimal (`0,5`) | Formatear siempre con `Locale.US` |
| `nCr(5,2)` no se podía escribir | No existía un botón de coma | Se añadió el botón `,` |
| Texto invisible con `2nd` activo | Texto del color de acento sobre un fondo del mismo color | Color de texto según el contraste del fondo |
| Vibración doble | La vibración se llamaba en el botón y en el manejador | Una sola llamada central |
| `2Ans` con Ans negativo daba un resultado incorrecto | `Ans` se sustituía sin paréntesis | Sustituir como `(valor)` |

- **Pruebas:** se añadieron pruebas unitarias del motor (`CalculatorEngineTest`).
- **v2.0.1:** se verificó la app en el emulador en horizontal. El resultado se cortaba y las etiquetas secundarias se encimaban, así que se ajustó el tamaño del display y se compactaron las etiquetas.

## 6. Versión completa (v3.0.0)

Se implementaron 15 mejoras planeadas, más varios extras.

**Números complejos.** Se creó `ComplexEngine.kt` con su propio analizador. `evaluateAny` intenta primero con números reales y, si falla (por ejemplo `√(-4)` o una expresión con `i`), usa los complejos. Así la calculadora normal no cambia de comportamiento.

**Cálculo en la graficadora:**
- derivada numérica por diferencias centrales
- integral con la regla de Simpson
- raíces e intersecciones por cambio de signo más bisección, descartando las asíntotas (donde |f| no tiende a 0)

**Matrices.** El determinante se calcula por eliminación gaussiana con pivoteo parcial, y la inversa por Gauss–Jordan.

**Fechas sin `java.time`.** `java.time` no existe en Android 7 sin configuración adicional, así que se implementó la conversión entre fecha y número de día con el algoritmo de días civiles (*days from civil*).

**Modo claro.** Los colores se convirtieron en propiedades que leen un estado de Compose (`Palette.light`), así que toda la app cambia al instante. La barra de estado también cambia de color con `WindowCompat`.

**Cursor editable.** Se usa `TextLayoutResult` para convertir la posición del toque en una posición del texto y para dibujar el cursor parpadeante.

**Widget.** `AppWidgetProvider` con `RemoteViews`. Se actualiza cada vez que cambia el historial.

**Ícono.** Ícono vectorial adaptativo para Android 8+ y una versión cuadrada en `mipmap-anydpi` para Android 7.

**Accesibilidad.** Cada botón tiene una descripción en español para TalkBack.

**Problema encontrado en el emulador:** los emojis del tutorial (🧮) se veían como cuadros vacíos porque Android 7 no los incluye. Se reemplazaron por símbolos matemáticos que existen en todas las versiones (`∑`, `⌫`, `∿`).

### Verificación

Cada función se probó en el emulador con Android 7.0 (vertical y horizontal, tema oscuro y claro) y con pruebas unitarias. Algunos resultados comprobados:

- `(2+3i)×(1−i) = 5 + i`
- `asin(1)` en RAD muestra `π/2`
- ∫₀^π x² dx = 10.335 (π³/3)
- intersecciones de x² y x+2 en x = −1 y x = 2
- el determinante de [[2,1],[1,3]] es 5, y su inversa es [[0.6, −0.2], [−0.2, 0.4]]

---

## Cómo revisar cualquier versión

```bash
git clone https://github.com/HugoDavidRS/Proyecto_Zarate_P2.git
cd Proyecto_Zarate_P2
git tag                    # lista las versiones
git checkout v1.0.0        # código de la primera calculadora
git checkout v1.2.0        # experimento DOOM con raycaster
git checkout main          # regresar a la versión actual
```

Cada versión compila por sí sola; se puede abrir y ejecutar en Android Studio.
