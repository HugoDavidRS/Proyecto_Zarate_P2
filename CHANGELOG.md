# Historial de cambios

Todos los cambios importantes del proyecto, de la versión más nueva a la más antigua.
El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y las versiones siguen [Versionado Semántico](https://semver.org/lang/es/).
Cada versión tiene una etiqueta en git; por ejemplo, `git checkout v2.0.0` restaura el código de esa versión.

## [3.0.0]

### Añadido
- **Números complejos** en la calculadora normal: `i` (pulsación larga en `e`), operaciones, potencias, `√` de negativos, `ln`, trigonometría, `abs`, `arg`, `conj`, `re` e `im`.
- **Graficadora mejorada**:
  - dos funciones a la vez, f(x) y g(x), con sus intersecciones
  - derivada y recta tangente en el punto tocado
  - integral definida con el área sombreada
- **Matrices** de 2×2 y 3×3: determinante, inversa, transpuesta, suma, resta, multiplicación y A².
- **Variables A–F** para guardar resultados y usarlos en expresiones (`2A + B`).
- **Finanzas**: interés compuesto año por año, préstamo con tabla de amortización, descuentos con IVA y propinas.
- **Fechas**: diferencia entre fechas (incluye días hábiles), edad exacta, próximo cumpleaños, signo zodiacal y sumar o restar días.
- **Primos**: si un número es primo, factorización, divisores, primo anterior y siguiente, y MCD/MCM de varios números.
- **Regresión lineal** en Estadística, con r, r² y predicción.
- **Formas exactas con π** (`π/2`, `3π/4`); al tocar la forma exacta se usa en la expresión.
- **Cursor editable** dentro de la expresión y **paréntesis faltantes** mostrados en gris.
- **Modo claro**, **decimales fijos** (0 a 8) y botón **Compartir historial**.
- **Widget** "Último resultado" para la pantalla de inicio.
- **Tutorial** de bienvenida y sección de ayuda en Ajustes.
- **Ícono propio** (vectorial), también para Android 7.
- **Accesibilidad**: descripciones en español para TalkBack.
- Más pruebas unitarias: complejos, variables, cálculo, matrices, fechas, primos, finanzas y regresión.

### Cambiado
- El nombre visible de la app ahora es **Calculadora Científica**.
- El fondo de arranque es oscuro, sin destello blanco.
- Los paneles de herramientas se separaron en `GraphPanel.kt` y `MoreTools.kt`.

## [2.0.1]

### Corregido
- Modo horizontal: el display es más alto para que el resultado no se corte, y las etiquetas secundarias se compactan (`⁻¹`) para no encimarse.

## [2.0.0]

### Añadido
- Herramientas: **constantes**, **conversor de unidades**, **estadística**, **bases numéricas**, **graficadora** con raíces, **ecuaciones** (cuadráticas y sistemas de 2×2) y **ajustes**.
- Botón **AC**, funciones secundarias con **pulsación larga** y etiqueta en la esquina, y **fracciones exactas**.
- **Modo horizontal** con una fila extra de funciones.
- **Historial persistente**: se puede tocar para reutilizar un resultado o mantener presionado para copiarlo.
- **Sonido de teclas**, **deslizar para borrar** y expresión con colores.
- 8 temas de color.
- Primeras pruebas unitarias del motor.

### Corregido
- El botón `√` siempre daba error.
- Con el teléfono en español los decimales salían con coma y el siguiente cálculo fallaba.
- No había botón de coma, así que no se podía escribir `nCr(5,2)`.
- Con `2nd` activo, el texto de `÷ × − +` era invisible.
- Cada tecla vibraba dos veces.
- `Ans` con un valor negativo calculaba mal.

## [1.3.1]

### Corregido
- El botón de tema no respondía; ahora cambia el color con un toque.

### Añadido
- Degradado en el display, línea de acento, animación del resultado y botones de operación con degradado.

## [1.3.0]

### Eliminado
- El botón y la pantalla de **DOOM** (ver el [procedimiento](docs/PROCEDIMIENTO.md#3-experimento-doom)).

### Añadido
- 4 temas de color, vibración al tocar, animación al presionar, sacudida en errores y copiar el resultado con pulsación larga.

## [1.2.0]

### Cambiado
- DOOM se reemplazó por un **motor raycaster propio** en HTML y JavaScript, sin dependencias ni internet.

## [1.1.x]

### Añadido
- Experimento: botón **DOOM** junto a DEG que abría el juego en un `WebView` con js-dos.

### Corregido
- Se intentó cargar DOOM desde archive.org.
- Se corrigió un cierre de la app por falta del permiso `ACCESS_NETWORK_STATE`.

## [1.0.0]

### Añadido
- Primera calculadora científica:
  - analizador de expresiones con precedencia de operadores y multiplicación implícita
  - trigonometría (normal, inversa e hiperbólica), logaritmos, raíces, factorial, nCr y nPr
  - modos DEG, RAD y GRAD, memoria, `Ans`, historial y botón `2nd`
  - diseño oscuro estilo iOS con el botón `=` de doble altura

## [0.1.0]

### Añadido
- Proyecto base creado con la plantilla **Empty Activity** (Jetpack Compose) de Android Studio.
