# ⚽ GoalTV - Reproductor IPTV, Guía Deportiva y EPG Cifrada

<p align="center">
  <img src="app/src/main/res/drawable/iconoredondo.webp" width="140" alt="GoalTV Logo" />
</p>

<p align="center">
  <b>Aplicación IPTV de alta rendimiento para Android TV, Fire TV, Teléfonos Móviles y Tablets.</b>
</p>

<p align="center">
  <a href="https://github.com/GoalSpain/goaltv/releases/latest"><img src="https://img.shields.io/github/v/release/GoalSpain/goaltv?color=orange&label=Última%20Versión" alt="GitHub Release"></a>
  <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/Platform-Android%20%7C%20Android%20TV-green" alt="Platform"></a>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Language-Kotlin%202.2-blue" alt="Language"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-purple" alt="Compose"></a>
</p>

---

## 🌟 Características Principales

### 📺 TV en Directo & EPG Sincronizada 1:1
* **Guía XMLTV Unificada (`guiaiptv.xml`)**: Sincronización horaria exacta en formato `Europe/Madrid`.
* **Indicadores en Tiempo Real**: Resaltado del programa activo con tono cálido, etiqueta **`EN DIRECTO`** y barra de progreso.
* **Auto-desplazamiento**: Posiciona automáticamente el cursor en la emisión en directo al seleccionar un canal.
* **Selección al Toque**: Tocar un canal selecciona su guía; tocar un canal seleccionado inicia la reproducción inmediatamente.

### ⚽ Programación Deportiva Actualizada al Día
* **Guía de Eventos Marca TV**: Actualizada automáticamente todos los días con peticiones en vivo.
* **Navegación Intuitiva**: Pestañas con fechas reales **`HOY (14 de Octubre)`** y **`MAÑANA (15 de Octubre)`**.
* **Navegación por Mando D-Pad**: Conmutación rápida entre días con las teclas izquierda/derecha del mando a distancia.

### 🎬 Cine y Series (VOD)
* **Portadas del Proveedor**: Carga de carátulas originales del servidor con soporte para rutas relativas.
* **Enriquecimiento TMDB**: Integración con The Movie Database para sinopsis, fechas de estreno y puntuaciones por estrellas.
* **Mosaico Adaptativo**: Rejilla optimizada que se escala automáticamente según el tamaño de la pantalla.

### 🛡️ VPN Integrada
* **Cifrado y Enrutamiento**: Pasarela VPN integrada para evitar bloqueos por parte de operadoras de internet.
* **Actualización Automática**: Consulta y actualización dinámica de contraseñas de red.
* **Acceso Multipantalla**: Conmutador accesible desde la pantalla de Inicio de Sesión, la Cabecera principal y Ajustes.

### 💾 Copia de Seguridad y Restauración Local
* **Buscador de Archivos del Sistema**: Exportación e importación de favoritos y categorías en formato JSON (`goaltv_backup.json`).
* **Reordenación de Categorías**: Reorganiza las categorías mediante botones dobles (`[ ⏫ ]` y `[ ⏬ ]`) y oculta canales no deseados.

---

## 📱 Capturas de Pantalla e Interfaz

* **Menú Principal y Búsqueda Maestra**
* **Guía EPG en Directo**
* **Programación Deportiva Diaria**
* **Gestión de Categorías y Ajustes**

---

## 🚀 Instalación y Descarga

Puedes descargar la última versión compilada lista para instalar en tu Smart TV o teléfono móvil desde la sección de Releases:

👉 **[Descargar GoalTV APK (Última Versión)](https://github.com/GoalSpain/goaltv/releases/latest)**

---

## 🛠️ Tecnologías y Arquitectura

* **Lenguaje**: [Kotlin](https://kotlinlang.org/)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material Design 3
* **Reproductor de Vídeo**: [Jetpack Media3 ExoPlayer](https://developer.android.com/guide/topics/media/exoplayer) (soporte para HLS, TS, MP4, MKV)
* **Carga de Imágenes**: [Coil](https://coil-kt.github.io/coil/) con cliente OkHttp personalizado para encabezados
* **Red y API**: Retrofit 2, OkHttp 3 & Moshi (JSON Parsing)
* **Arquitectura**: MVVM (Model-View-ViewModel) + Pattern Repository + Kotlin Coroutines & StateFlow

---

## 📝 Licencia

Este proyecto está distribuido bajo la licencia pública para uso personal y comunitario.

Developed with ❤️ by **GoalSpain**.
