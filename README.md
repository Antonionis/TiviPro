# ⚽ PolTivi - Reproductor IPTV con Guía Deportiva y EPG

<p align="center">
  <img src="app/src/main/res/drawable/logo.png" width="140" alt="PolTivi Logo" />
</p>

<p align="center">
  <b>Aplicación IPTV de alto rendimiento para Android TV, Fire TV, Teléfonos Móviles y Tablets.</b>
</p>

<p align="center">
  <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/Platform-Android%20%7C%20Android%20TV-green" alt="Platform"></a>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Language-Kotlin%202.2-blue" alt="Language"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-purple" alt="Compose"></a>
</p>

> [!IMPORTANT]
> ⚠️ **PolTivi no proporciona listas de reproducción ni ningún otro contenido digital.**

---

## 🌟 Características Principales

### 📺 TV en Directo & EPG Sincronizada
* **Guía XMLTV Unificada**: Sincronización horaria exacta en formato `Europe/Madrid`.
* **Indicadores en Tiempo Real**: Resaltado del programa activo con tono cálido, etiqueta **`EN DIRECTO`** y barra de progreso.
* **Auto-desplazamiento**: Posiciona automáticamente el cursor en la emisión en directo al seleccionar un canal.
* **Selección al Toque**: Tocar un canal selecciona su guía; tocar un canal seleccionado inicia la reproducción inmediatamente.

### ⚽ Programación Deportiva Actualizada al Día
* **Guía de Eventos**: Actualizada automáticamente todos los días.
* **Navegación Intuitiva**: Pestañas con fechas reales **`HOY`** y **`MAÑANA`**.
* **Navegación por Mando D-Pad**: Conmutación rápida entre días con las teclas izquierda/derecha del mando a distancia.

### 🎬 Cine y Series (VOD)
* **Portadas del Proveedor**: Carga de carátulas originales del servidor con soporte para rutas relativas.
* **Enriquecimiento TMDB**: Integración con The Movie Database para sinopsis, fechas de estreno y puntuaciones por estrellas.
* **Mosaico Adaptativo**: Rejilla optimizada que se escala automáticamente según el tamaño de la pantalla.

### 💾 Copia de Seguridad y Restauración Local
* **Buscador de Archivos del Sistema**: Exportación e importación de favoritos y categorías en formato JSON (`poltivi_backup.json`).
* **Reordenación de Categorías**: Reorganiza las categorías mediante botones dobles (`[ ⏫ ]` y `[ ⏬ ]`) y oculta grupos de canales no deseados.

---

## 📱 Capturas de Pantalla e Interfaz

* **Menú Principal**
<p align="center">
  <img src="images/principal.jpg" width="500" alt="principal" />
</p>
* **Guía EPG en Directo**
<p align="center">
  <img src="images/epg.jpg" width="500" alt="epg" />
</p>
* **Programación Deportiva Diaria**
<p align="center">
  <img src="images/prog.jpg" width="500" alt="programacion" />
</p>
* **Gestión de Categorías y Ajustes**
<p align="center">
  <img src="images/gestioncat.jpg" width="500" alt="categorias" />
</p>
<p align="center">
  <img src="images/ajustes.jpg" width="500" alt="ajustes" />
</p>

---

## 🚀 Instalación y Descarga

Puedes descargar la última versión compilada lista para instalar en tu Smart TV o teléfono móvil desde la sección de Releases:

👉 **[Descargar PolTivi APK (Última Versión)](https://github.com/poltivi/poltivi/releases/latest)**

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

Developed with ❤️ by **PolTivi**.
