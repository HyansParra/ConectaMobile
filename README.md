# ConectaMobile 📱💬

**Asignatura:** Programación Android
**Carrera:** Analista Programador
**Desarrollado por:** Hyans Nicolás Parra Valdivia

## 📋 Descripción Técnica
Aplicación nativa Android desarrollada en Java 17 que implementa una arquitectura de mensajería híbrida. Integra servicios en la nube (**Firebase**) con protocolos de comunicación ligera en tiempo real (**MQTT**) para garantizar la entrega de mensajes y la persistencia de datos.

## 🏗️ Arquitectura del Sistema
El proyecto sigue el patrón de diseño **MVC (Modelo-Vista-Controlador)** nativo de Android:
* **Modelo:** Clases POJO (`User.java`, `Message.java`) que definen la estructura de datos.
* **Vista:** Layouts XML y adaptadores (`ChatAdapter`) que gestionan la presentación visual.
* **Controlador:** Activities (`ChatActivity`, `MainActivity`) que gestionan la lógica de negocio.



## 📡 Protocolos y Comunicaciones

### 1. MQTT (Message Queuing Telemetry Transport)
Se seleccionó MQTT sobre HTTP/REST por su eficiencia en entornos móviles:
* **Bajo Consumo:** Mantiene una conexión TCP persistente que reduce el gasto de batería.
* **Baja Latencia:** Permite la recepción inmediata de mensajes (*Push*).
* **Interoperabilidad:** Canal público (`conectamobile/global`) para pruebas externas.

### 2. Firebase Realtime Database
Capa de persistencia utilizada para:
* Historial de chat.
* Gestión de usuarios y fotos.
* Sincronización Offline.

## 🛠️ Dependencias y Justificación Técnica

### Cliente MQTT: `Hannesa2/paho.mqtt.android`
**Versión:** `4.3.beta1`
**Justificación:** Se utiliza este fork específico en lugar de la librería estándar de Eclipse Paho o versiones anteriores porque es la única compatible con **Android 14 (API 34)**. Implementa `WorkManager` y cumple con los permisos `FOREGROUND_SERVICE_DATA_SYNC`, evitando cierres inesperados por restricciones del sistema operativo.

### Imágenes: `Glide`
**Versión:** `4.16.0`
**Justificación:** Librería recomendada por Google para la carga asíncrona de imágenes, manejo eficiente de memoria y caché de fotos de perfil.

## ✅ Funcionalidades Clave
* Login/Registro con Firebase Auth.
* Gestión de Perfil (Subida de foto a Storage).
* Chat Privado y Global.
* Persistencia Offline activada.
* Soporte para Modo Oscuro.

---
**Fecha de Entrega:** 04 de Diciembre de 2025