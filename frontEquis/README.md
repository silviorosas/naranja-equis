# FrontEquis 🍊 (NaranjaX UI)

Bienvenido a **FrontEquis**, la interfaz de usuario moderna y estilizada para el ecosistema de billetera virtual NaranjaX. 

## ✨ Características Premium

- **📱 Mobile-First Design**: Diseñado específicamente para celulares, con un layout que se adapta fluidamente a Desktop.
- **⚡ Experiencia Dinámica**:
    - **Toasts NaranjaX**: Sistema de notificaciones profesionales para Feedback instantáneo.
    - **Modales Overlays**: Paneles de ingreso y transferencia con desenfoque de fondo y centrado perfecto.
    - **Bottom Navigation**: Barra de navegación inferior en móviles para mejor accesibilidad (estilo nativo).
- **🇦🇷 Localización Argentina**:
    - Formato de moneda en Pesos Argentinos (`ARS`).
    - Horarios sincronizados con GMT-3 (Buenos Aires).
    - Ordenamiento de movimientos: Los más recientes aparecen primero.

## 🛠️ Tecnologías

- **Angular 18** (Components Standalone)
- **ngx-toastr** (Notifications)
- **CSS3 / Flexbox / Grid** (Estilo a medida)
- **FontAwesome 6** (Iconografía)
- **RxJS** (Gestión de flujos de datos)

## 🚀 Instalación y Desarrollo

1. **Instalar dependencias**:
   ```bash
   npm install
   ```

2. **Iniciar servidor de desarrollo**:
   ```bash
   ng serve
   ```
   La aplicación estará disponible en `http://localhost:4200`.

## 📂 Estructura del Feature Dashboard

El **Dashboard** es el núcleo de la aplicación, organizado para claridad y mantenimiento:
- `dashboard.component.ts`: Lógica de ordenamiento y filtrado de transacciones.
- `dashboard.component.html`: Layout basado en tarjetas y grillas optimizadas.
- `dashboard.component.css`: Estilos específicos con media-queries para iPhone y Monitores 1080p.

## 🛡️ Seguridad
La aplicación consume servicios protegidos. Utiliza un **AuthInterceptor** para adjuntar el token JWT a todas las peticiones salientes hacia el API Gateway.

---
**Diseñado con ❤️ para Silvio Rosas (NaranjaX Training)**  
**Versión**: 1.2.0 (Premium UI Refactored)
