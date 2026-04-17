# Plan de Continuación - AgroPulse v6.1+

## 🎯 Objetivos Completados en Esta Sesión

### ✅ 1. Menús Funcionales
- [x] Arreglada página de Soporte
- [x] Arreglada página de Configuración
- [x] Arreglada página de Ayuda
- [x] Todas las opciones del submenú funcionan

### ✅ 2. Carga de Datos del ESP32
- [x] Dashboard carga datos del backend REST
- [x] Mostrados sensores en tiempo real
- [x] Actualización automática cada 5 segundos
- [x] Gráficos funcionales

### ✅ 3. API Completa
- [x] Todos los endpoints implementados
- [x] CRUD de sensores, cultivos, actuadores, alertas
- [x] Lectura de datos en tiempo real

### ✅ 4. Mejoras Visuales
- [x] Colores mejorados (gradientes verdes)
- [x] Sidebar con estilo moderno
- [x] Header con gradiente
- [x] Cards con sombras y efectos hover
- [x] Tipografía mejorada

---

## 🚀 Próximas Tareas (Fase 2)

### 1. **SINCRONIZACIÓN BIDIRECCIONAL** (Crítico)
```
⏳ Permita cambiar cultivos desde web
  - GET /api/crops/{id} ✅
  - PUT /api/crops/{id} ⏳
  - DELETE /api/crops/{id} ⏳

⏳ Permita controlar actuadores desde web
  - GET /api/actuators/{id} ✅
  - PUT /api/actuators/{id} ✅
  - DELETE /api/actuadores/{id} ✅

⏳ Sincronización con app desktop
  - Ambas apps lean del mismo backend
  - Cambios se reflejen en tiempo real
```

**Estimado**: 2-3 horas

### 2. **EDICIÓN DE CULTIVOS** (Importante)
```
⏳ Permitir editar cultivos
  - Modal o formulario inline
  - Actualizar rangos de T°, Humedad, etc.
  - Eliminar cultivos

⏳ Cambiar cultivo activo
  - Selector fácil de acceder
  - Confirmación antes de cambiar
  - Efecto visual del cambio
```

**Estimado**: 1.5-2 horas

### 3. **CONTROL DE ACTUADORES** (Importante)
```
⏳ Encender/apagar desde web
  - UI mejorada (toggles visuales)
  - Modo manual/automático
  - Historial de cambios

⏳ Programar actuadores
  - Horarios específicos
  - Condiciones por sensor
  - Reglas automáticas
```

**Estimado**: 2-3 horas

### 4. **GRÁFICOS AVANZADOS** (Mejora Visual)
```
⏳ Dashboard con 4 gráficos principales
  - Temperatura vs Tiempo (línea)
  - Humedad vs Tiempo (área)
  - Comparativa Temp (interior vs exterior)
  - Humedad suelo vs Tiempo

⏳ Selección de rango temporal
  - Últimas 24 horas (default)
  - Últimos 7 días
  - Últimos 30 días
  - Rango personalizado
```

**Estimado**: 3-4 horas

---

## 📋 TAREAS MENORES

### UI/UX Refinamientos
- [ ] Mejor handling de estados vacíos (empty states)
- [ ] Loading spinners más bonitos
- [ ] Notificaciones toast (éxito/error)
- [ ] Skeleton loaders en cards
- [ ] Animaciones de entrada suave (stagger)

### Validaciones
- [ ] Validar rangos en cultivos (min < max)
- [ ] Validación de emails
- [ ] Confirmación antes de eliminar
- [ ] Mensajes de error claros

### Responsividad
- [ ] Tablets: 2-3 columnas en grid
- [ ] Mobile: 1 columna, sidebar oculto
- [ ] Botones más grandes en móvil
- [ ] Touch-friendly spacing

---

## 🔄 FLUJO DE DATOS ACTUAL

```
ESP32 → Backend Java → /api/readings → Web App
                  ↓
             SQLite Database
                  ↑
         Desktop App (lectura)
```

## 🔄 FLUJO DE DATOS MEJORADO (Objetivo)

```
ESP32 ←→ Backend Java ←→ Web App
                ↓
         SQLite Database
                ↑
         Desktop App ←→ Cambios
```

---

## 💾 ARCHIVOS A MODIFICAR

### Frontend (Web)
```
webapp/src/
├── App.jsx              ← Componentes principales
├── services/
│   └── api-client.ts    ← Endpoints (completado ✅)
├── pages/               ← Por crear si es necesario
└── components/          ← Por crear si es necesario
```

### Backend (Java)
```
src/main/java/com/agropulse/
├── api/
│   ├── CropRestController.java      ← Implementar PUT/DELETE
│   ├── ActuatorRestController.java  ← Mejoras
│   └── ReadingRestController.java   ← Agregar filtros
├── service/
│   └── esp32/                       ← Ya implementado ✅
└── dao/
    ├── CropDao.java                 ← Update/Delete
    └── ActuatorDao.java             ← Update/Delete
```

---

## 🧪 TESTING RECOMENDADO

### Tests Manuales
- [ ] Crear cultivo y verificar en web
- [ ] Editar cultivo y verificar cambio inmediato
- [ ] Activar actuador y verificar estado
- [ ] Cambiar cultivo y verificar rangos en cards
- [ ] Crear alerta y recibir notificación

### Tests de Integración
- [ ] Cambios en web aparecen en desktop
- [ ] Cambios en desktop aparecen en web
- [ ] Sincronización en tiempo real
- [ ] Conflictos de cambios simultáneos

---

## 📊 PRIORIDAD DE TAREAS

```
CRÍTICA (Esta semana)
├─ Edición de cultivos
├─ Control de actuadores
└─ Sincronización bidireccional

IMPORTANTE (Próxima semana)
├─ Gráficos avanzados
├─ Reportes
└─ Exportar datos

OPCIONAL (Después)
├─ Tema oscuro
├─ Push notifications
└─ App móvil nativa
```

---

## 🎯 MILESTONES

**v6.0** (Hoy ✅)
- Menús funcionales
- Lectura de datos ESP32
- UI mejorada

**v6.1** (Esta semana)
- Editar cultivos/actuadores
- Control remoto
- Gráficos avanzados

**v6.2** (Próxima semana)
- Reportes y estadísticas
- Sincronización perfecta
- Optimización móvil

**v7.0** (Mes siguiente)
- App móvil
- Integraciones externas
- Features avanzadas

---

## 📝 NOTAS IMPORTANTES

1. **Test en vivo**: Los cambios deben reflejarse en < 5 segundos
2. **Base de datos**: SQLite se sincroniza automáticamente
3. **API**: Usar JSON en requests/responses
4. **Seguridad**: Validar permisos en backend
5. **Performance**: Implementar caching si es necesario

---

**Documento creado**: 2026-04-17 16:00 UTC
**Versión del plan**: 1.0
**Próxima revisión**: 2026-04-20
