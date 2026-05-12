# TripPlanner ✈️

Aplicación Android para la planificación colaborativa de viajes, desarrollada con Kotlin y Jetpack Compose como Proyecto Intermodular del ciclo DAM.

## 📱 Descripción

TripPlanner permite planificar viajes de forma colaborativa con amigos y familia. Incluye gestión de itinerarios, gastos compartidos, mapa interactivo de puntos de interés y exploración de destinos turísticos.

## 🚀 Funcionalidades principales

- Crear y gestionar viajes con fechas y presupuesto.
- Planificar actividades en un itinerario compartido en tiempo real.
- Gestionar y repartir gastos entre participantes.
- Marcar puntos de interés en un mapa interactivo (MapLibre + MapTiler).
- Explorar destinos turísticos con Nominatim y Overpass API.
- Notificaciones colaborativas cuando otros participantes añaden contenido.
- Modo oscuro / claro.
- Acceso offline con caché local Room.

## 🛠️ Tecnologías

- **Kotlin** + **Jetpack Compose**
- **Firebase Auth** + **Firebase Firestore**
- **Room** (base de datos local)
- **Jetpack DataStore** (preferencias)
- **MapLibre** + **MapTiler**
- **WorkManager** (notificaciones periódicas)
- **Coil** (carga de imágenes)
- **Dokka** (documentación)

## 📚 Documentación

La documentación del código generada con Dokka está disponible en:

🔗 https://lcp622.github.io/TripPlanner/

## ⚙️ Instalación

### Opción A — APK directa

1. Descarga el APK desde la carpeta `app/release/`, o bien, descarga el APK incluido directamente en la tarea.
2. En el dispositivo Android activa la opción de instalar aplicaciones de fuentes desconocidas.
3. Instala el APK y abre la app.
4. Usa las credenciales de pruebab (adjuntadas en el envío de la tarea).

### Opción B — Android Studio

1. Clona el repositorio:
2. Abre el proyecto en **Android Studio**
3. Crea el archivo `local.properties` en la raíz del proyecto con las APIs incluidas en la documentación, u obtén una nueva en la página web oficial.
4. Espera a que Gradle descargue las dependencias (**Sync Now**)
5. Conecta un dispositivo o inicia un emulador y pulsa **Run ▶**.
6. Usa las credenciales de prueba

Los datos de prueba están precargados en Firebase Firestore con viajes, actividades y gastos de ejemplo.

## 🧪 Pruebas

El proyecto incluye:
- **15 pruebas de unidad** en `app/src/test/` → ejecutar con clic derecho → *Run Tests*
- **1 prueba de integración** en `app/src/androidTest/` → requiere dispositivo o emulador

## 📁 Estructura del proyecto
dam.pmdm.tripplanner/
├── data/
│   ├── local/       → Room: DAOs, entidades, base de datos
│   └── repository/  → Repositorios Firestore + Room
├── ui/
│   ├── auth/        → Login y registro
│   ├── viajes/      → Viajes, detalle, rutas, participantes
│   ├── itinerario/  → Actividades
│   ├── gastos/      → Gastos y repartos
│   ├── perfil/      → Perfil de usuario
│   └── theme/       → Colores y estilos
├── MainActivity.kt
├── NavGraph.kt
└── NotificacionWorker.kt

## 👩‍💻 Autora

Lucía — DAM Intermodular 2025/2026

## 📄 Licencia

Proyecto académico — todos los derechos reservados.
