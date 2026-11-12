# Aura — launcher minimalista para Android TV

Launcher en Kotlin + Compose for TV pensado para una tele JVC con Android TV.
Una sola fila de apps, reloj discreto y un foco sobrio. Sin recomendaciones ni anuncios.

## Qué incluye

- Fondo oscuro con degradado suave y reloj/fecha arriba a la izquierda.
- Fila de apps (banner 16:9 si la app lo tiene, icono si no), ordenadas alfabéticamente.
- Nombre de la app enfocada bajo la fila, con transición suave.
- Tarjeta final de **Ajustes** para llegar a los ajustes del sistema.
- La lista se actualiza sola al instalar o desinstalar apps.
- El botón Atrás no cierra el launcher.
- **Seguir viendo**: una fila con lo último que has reproducido en cada app de streaming
  (carátula, título y barra de progreso). Pulsación larga sobre una tarjeta para quitarla.

## Seguir viendo: cómo funciona y sus límites

La fila nativa "Continuar viendo" de Android TV (Watch Next) solo puede leerla el launcher
del propio sistema; una app de terceros no recibe ese permiso. Por eso Aura hace otra cosa:
observa las **sesiones multimedia** (las mismas que alimentan los controles de reproducción
del sistema) y guarda título, carátula y progreso de lo que reproduces.

Qué implica:

- Solo aparece lo que veas **a partir de instalar Aura**. No recupera tu historial anterior.
- Depende de que cada app publique sus metadatos. Netflix, YouTube y similares suelen hacerlo,
  pero algunas apps dan poco (sin carátula, o sin duración y por tanto sin barra de progreso).
- Al pulsar una tarjeta se **abre la app**, no el capítulo exacto. Android no expone un enlace
  de reanudación para apps ajenas; cada app te mostrará su propio "continuar".
- Se guarda una entrada por app y las apps de solo audio (Spotify, YouTube Music…) se
  ignoran. La lista está en `IGNORED_PACKAGES`, dentro de `MediaWatchService.kt`.
- Todo queda en la tele: nada se envía a ningún sitio.

### Dar permiso (una sola vez)

Aura necesita "acceso a notificaciones" para poder leer las sesiones multimedia. Hasta que lo
concedas, la fila muestra una tarjeta "Activar «Seguir viendo»". En muchas teles esa pantalla
de ajustes no existe, así que lo más fiable es concederlo por ADB:

```bash
adb shell cmd notification allow_listener com.aura.launcher/com.aura.launcher.service.MediaWatchService
```

Para revocarlo:

```bash
adb shell cmd notification disallow_listener com.aura.launcher/com.aura.launcher.service.MediaWatchService
```

## Compilar

1. Abre la carpeta `Aura` con Android Studio (Ladybug o posterior).
2. Deja que sincronice Gradle. Si falta el wrapper, Android Studio lo genera; el proyecto usa Gradle 8.9.
3. Genera el APK: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
   Queda en `app/build/outputs/apk/debug/app-debug.apk`.

### Sin instalar nada: APK con GitHub Actions

El proyecto incluye `.github/workflows/build-apk.yml`, que compila el APK en los servidores de GitHub:

1. Crea un repositorio en GitHub (puede ser privado) y sube la carpeta `Aura` a la rama `main`.
2. Abre la pestaña **Actions**, elige **Build APK** y pulsa **Run workflow** (también se lanza solo
   al hacer push a `main`).
3. Cuando termine (unos minutos), entra en la ejecución y descarga **Aura-debug-apk** al final de la página.
   Es un zip que contiene `app-debug.apk`.

## Instalar en la tele sin ordenador (recomendado)

Con el repositorio **público** en GitHub, cada compilación deja el APK en Releases con un enlace fijo:

```
https://github.com/USUARIO/REPOSITORIO/releases/latest/download/Aura.apk
```

1. En la tele, instala **Downloader** (de AFTVnews) desde Google Play.
2. Permite instalar apps de fuentes desconocidas para Downloader (la tele te lleva al ajuste la primera vez).
3. Abre Downloader, escribe el enlace de arriba y pulsa Ir. Cuando termine la descarga, pulsa **Instalar**.
4. Aura aparecerá en tus apps. Para usarla como pantalla de inicio, mira el apartado siguiente.

## Instalar en la tele con ADB

1. En la tele: `Ajustes > Preferencias del dispositivo > Acerca de`, pulsa varias veces sobre "Compilación" para activar el modo desarrollador.
2. Activa `Opciones de desarrollador > Depuración USB` (y, si aparece, `Depuración por red`).
3. Desde el ordenador, con la tele en la misma red:

```bash
adb connect IP_DE_LA_TELE:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Usarlo como pantalla de inicio

Al pulsar el botón Home, Android debería preguntar qué launcher usar. Si tu JVC no lo
pregunta o lo ignora, hay dos caminos:

- `Ajustes > Aplicaciones > Aplicaciones predeterminadas` (o "Pantalla de inicio"), si tu versión lo ofrece.
- Desactivar el launcher original **solo para tu usuario** (reversible):

```bash
adb shell pm list packages | grep -i -E "launcher|tvlauncher|home"
adb shell pm disable-user --user 0 PAQUETE_DEL_LAUNCHER_ORIGINAL
```

Para volver atrás:

```bash
adb shell pm enable PAQUETE_DEL_LAUNCHER_ORIGINAL
```

Apunta el nombre del paquete antes de desactivarlo. En muchas teles el original se llama
parecido a `com.google.android.tvlauncher`, pero en JVC puede variar.

## Estructura

```
app/src/main/java/com/aura/launcher/
  MainActivity.kt        actividad HOME, receptor de instalaciones
  HomeViewModel.kt       estado de la lista de apps
  data/AppRepository.kt  consulta de apps (Leanback + Launcher)
  data/AppEntry.kt       modelo de app
  data/WatchEntry.kt     modelo de "Seguir viendo"
  data/WatchHistoryStore.kt  historial local (JSON + carátulas)
  service/MediaWatchService.kt  lee las sesiones multimedia
  ui/HomeScreen.kt       pantalla principal
  ui/theme/Theme.kt      colores y tipografía
```

## Siguientes ideas

- Ocultar o reordenar apps (pulsación larga) y guardarlo con DataStore.
- Elegir en ajustes qué apps cuentan como "streaming" en vez de la lista fija.
- Fondo personalizable o fotos de ambiente discretas.
- Segunda fila de accesos rápidos (entradas HDMI, Netflix, YouTube).
- Barra superior con estado de red.
