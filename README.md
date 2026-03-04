# 🍽️ Backend Factu - Sistema de Gestión de Restaurante

Este proyecto es una solución de **backend** diseñada para centralizar la operativa de un establecimiento gastronómico, abarcando desde la toma de pedidos hasta la generación de facturas.

El sistema está optimizado para funcionar en una infraestructura **cloud**, permitiendo la sincronización de datos en tiempo real entre el área de servicio y la administración.

---

## 🚀 Características Principales

El sistema gestiona de forma integral el flujo de negocio del restaurante:

- 📋 **Catálogo de Productos**  
  Gestión dinámica de categorías, precios y fotos para el menú.

- 🍳 **Gestión de Comandas**  
  Control del estado de los pedidos y comunicación con cocina.

- 🪑 **Administración de Mesas**  
  Organización del local por secciones y disponibilidad.

- 💳 **Módulo de Facturación**  
  Registro de ventas con múltiples métodos de pago.

- 🔐 **Seguridad y Acceso**  
  Control de perfiles mediante autenticación con JWT.

---

## 🛠️ Stack Tecnológico

Implementado con herramientas modernas para garantizar estabilidad y seguridad:

- **Lenguaje:** Java 17+  
- **Framework:** Spring Boot  
  - Spring Security  
  - Spring Data JPA  
- **Base de Datos:** PostgreSQL (Supabase - Cloud)  
- **Seguridad:** JSON Web Tokens (JWT)  
- **Gestión de Dependencias:** Maven  

---

## 🗄️ Estructura de Datos

El sistema organiza la información en **14 tablas principales**:

- 👤 Usuarios y perfiles → Control de acceso
- 🍔 Categorías y productos → Menú e inventario
- 🪑 Secciones y mesas → Distribución del local
- 🧾 Pedidos y facturación → Flujo de ventas

---

## ⚙️ Instalación y Ejecución

```bash
# Clonar repositorio
git clone https://github.com/tu-usuario/tu-repo.git

# Entrar al proyecto
cd tu-repo

# Ejecutar
mvn spring-boot:run
