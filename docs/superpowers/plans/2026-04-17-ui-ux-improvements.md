# Plan de Mejora UI/UX - AgroPulse Web v6.1

## 🎨 Principios de Diseño

### Color Palette Mejorado
```
Primario:      #10B981 (Emerald - natural, agrícola)
Secundario:    #F59E0B (Amber - luz, advertencias)
Terciario:     #3B82F6 (Blue - frío/agua)
Éxito:         #10B981 (Green)
Advertencia:   #F59E0B (Yellow)
Error:         #EF4444 (Red)
Fondo:         #F9FAFB (Gray-50)
Superficie:    #FFFFFF (White)
Texto:         #1F2937 (Gray-800)
Texto Suave:   #6B7280 (Gray-600)
```

### Tipografía
- **Headers**: Poppins Bold (700)
- **Body**: Inter Regular (400)
- **Monospace**: IBM Plex Mono (código/datos)
- **Tamaño base**: 16px → escala 1.125

## 📐 Layout Mejoras

### Dashboard (Home)
```
┌─────────────────────────────────────────────┐
│ Welcome Header + Quick Stats                 │
├─────────────────────────────────────────────┤
│  [Temp]  [Humidity]  [Soil]  [Status]      │
│  Cards con animaciones y gradientes         │
├─────────────────────────────────────────────┤
│  Gráfico en tiempo real (24h)               │
│  ├─ Linea suave para Temperatura           │
│  └─ Area para Humedad                       │
├─────────────────────────────────────────────┤
│  Cultivo Activo + Alertas (lado a lado)    │
└─────────────────────────────────────────────┘
```

### Componentes Globales
- **Header**: Sticky, gradiente, shadow sutil
- **Sidebar**: Collapsible, smooth animations
- **Cards**: Gradient backgrounds, hover effects
- **Buttons**: Icono + texto, states visuales (active/disabled)
- **Forms**: Labels flotantes, validación en tiempo real

## ✨ Características Visuales

### Cards Mejoradas
- Gradientes sutiles basados en tipo de sensor
- Indicadores visuales (dot de estado)
- Valores grandes y legibles
- Unidades pequeñas y grises
- Barras de progreso para rangos

### Animaciones
- Fade-in al cargar datos
- Pulse suave en valores actualizados
- Slide en notificaciones
- Smooth transitions (200ms)
- Hover effects en botones

### Iconografía
- Lucide React (ya instalado)
- Coherencia visual en toda la app
- Tamaños: 16px (pequeño), 20px (normal), 24px (grande)

## 🎯 Mejoras por Página

### Dashboard (✅ Prioritario)
1. Cards con gradientes y shadow mejorado
2. Gráfico responsivo y suave
3. Estado del cultivo prominente
4. Alertas con animación de slide

### Sensores
1. Grid responsive 2-4 columnas
2. Cards con estado visual
3. Botones de acción mejorados
4. Empty state ilustrado

### Actuadores
1. Toggle switches animados
2. Modo automático/manual visual
3. Cards con estado (on/off) con color
4. Control intuitivo

### Cultivos
1. Cards grandes con detalles
2. Rangos visualizados con barras
3. Íconos de estado
4. Editar inline o modal

### Configuración
1. Secciones claramente separadas
2. Inputs con validación visual
3. Status indicators
4. Confirmaciones claras

## 📱 Responsividad

- **Mobile**: 1 columna, sidebar oculto
- **Tablet**: 2 columnas, sidebar colapsible
- **Desktop**: 4+ columnas, sidebar abierto
- **Grandes**: Max-width 1400px

## 🔄 Transiciones

```css
transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1)
/* Smooth out, ease in-out */
```

## 🎬 Animaciones Clave

1. **Page Load**: Fade in + Stagger children
2. **Data Update**: Subtle pulse + number change
3. **Alerts**: Slide + glow
4. **Buttons**: Scale + color change on hover
5. **Modals**: Backdrop fade + scale modal

## 🚀 Implementación

### Fase 1: Colores y Sombras (1-2 horas)
- Actualizar palette tailwind
- Aplicar a componentes existentes

### Fase 2: Animaciones (1-2 horas)
- Transiciones suaves
- Micro-interactions

### Fase 3: Gráficos (2-3 horas)
- Recharts mejorado
- Responsive y animado

### Fase 4: Componentes (2-3 horas)
- Cards rediseñadas
- Botones y inputs

### Fase 5: Responsive (1-2 horas)
- Breakpoints mobile
- Optimización táctil

---

**Estimado Total**: 7-12 horas de trabajo
**Prioridad**: Dashboard > Actuadores > Cultivos > Sensores
