# 📝 Reminders

**Un sistema de gestión de recordatorios personales con integración de datos meteorológicos.**

## 1. 🧑‍💻 Integrantes del Equipo

* **Emerson Puebla**
* **Felipe Moreria**

## 2. ✨ Funcionalidades Principales

### Backend (Microservicio)
* **Gestión de Recordatorios (CRUD):** Permite crear, obtener, actualizar y marcar como eliminados los recordatorios (`Reminder`).
* **Gestión de Recursos Relacionados:** Manejo de archivos adjuntos (`Attachment`) y grabaciones de voz (`Audio`).
* **Estandarización de Respuestas:** Todas las respuestas de la API (`2xx`, `4xx`, `5xx`) utilizan un formato estandarizado (`ApiResponse`/`ApiErrorResponse`).
* **Delete:** Los datos se marcan como eliminados (`isDeleted: true`).

### Aplicación Móvil (Android)
* **CRUD de Recordatorios:** Interfaz completa para la creación, visualización, edición y eliminación de recordatorios.
* **Integración de Clima:** Muestra la temperatura y datos meteorológicos relevantes usando OpenWeather.
* **Configuración de Conexión:** Permite configurar la URL del backend mediante ajustes manuales o escaneo QR.

### Repositorios de GitHub

| Proyecto | Repositorio |
| :--- | :--- |
| **Backend** | `EmersonPuebla/reminders_backend` |
| **App Móvil** | `EmersonPuebla/reminders` |


## 3. 🌐 Endpoints Usados

### Endpoints Propios (Microservicio)

| Recurso | Método | Endpoint | CRUD |
| :--- | :--- | :--- | :--- |
| **Reminder** | `POST` | `/api/v1/reminder` | **C**reate |
| **Reminder** | `GET` | `/api/v1/reminder/{id}` | **R**ead |
| **Reminder** | `PUT` | `/api/v1/reminder/{id}` | **U**pdate |
| **Reminder** | `DELETE` | `/api/v1/reminder/{id}` | **D**elete |

### Endpoints Externos

| Servicio | Propósito |
| :--- | :--- |
| **OpenWeatherMap** | Obtiene el clima y la temperatura para la ubicación del usuario. |

## 4. 🛠️ Instrucciones para Ejecutar el Proyecto

### Requisitos
* Java Development Kit (JDK) 21
* Android Studio
* Acceso a Gradle

### Backend (Spring Boot / Kotlin)
1.  **Clonar el repositorio:** `git clone https://github.com/EmersonPuebla/reminders_backend.git`
2.  **Ejecutar:** Iniciar la aplicación desde el IDE (IntelliJ/VS Code) o usando `./gradlew bootRun`.
3.  El servicio estará disponible en `http://localhost:8080`.

### Aplicación Móvil (Android Studio)
1.  **Abrir Proyecto:** Abrir el directorio `reminders` en Android Studio.
2.  **Sincronizar Gradle:** Esperar la sincronización de Gradle para resolver las dependencias.
3.  **Ejecutar:** Presionar **Run** para desplegar en un emulador o dispositivo físico.

## 5. ⚙️ Configuración de Conexión (App Móvil)

La aplicación móvil permite configurar la URL del backend a través de la pantalla de Ajustes. Esta configuración genera la URL base final: `{Protocolo}://{Dirección}:{Puerto}/api/v1/`.

### Opciones de Configuración
1.  **Manual:** Campos de texto para setear el Protocolo (`http/https`), la Dirección (Host) y el Puerto.
2.  **QR Code:** La aplicación puede:
    * **Generar/Compartir:** Mostrar un QR Code que codifica la configuración de conexión actual.
    * **Leer/Escanear:** Usar la cámara para escanear un QR y aplicar automáticamente la nueva configuración.

## 6. 📦 Entrega y Firma

| Archivo | Ubicación | Descripción |
| :--- | :--- | :--- |
| **APK Firmado** | `/release/app-release.apk` | Archivo de instalación para Android. |
| **Archivo JKS** | `/signing/reminders.jks` | Archivo KeyStore utilizado para firmar la aplicación. |
