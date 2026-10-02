# Decisiones

## Diseño actual

- El paquete `com.store.inventory.api` es el contrato público y no se modifica.
- Por ahora, los datos viven en memoria. `InventoryServiceImpl` guarda los productos usando el SKU como clave.
- Registrar un SKU por segunda vez conserva el producto original, incluida su categoría y stock.
- `ProductCategoryRules` centraliza las reglas de reserva de cada categoría.
- Si se repite un `orderId` con una reserva activa, el servicio devuelve la reserva existente. Así, un reintento no reserva unidades dos veces.
- El contrato público usa un `enum` fijo. Por eso, esta versión solo soporta `STANDARD`, `PRE_ORDER` y `FLASH_SALE`; no se pueden crear categorías en tiempo de ejecución.

## Concurrencia

- `synchronized` protege los datos en memoria dentro de una sola instancia del servicio. No coordina el stock entre varias instancias, por lo que esta implementación podría vender más unidades de las disponibles.

## Alertas de stock

- El servicio detecta bajo stock cuando hay 5 unidades disponibles o menos y llama una vez a `StockAlertListener`.
- La alerta se habilita de nuevo cuando el stock disponible supera 5 unidades, ya sea por reposición o por vencimiento de una reserva.
- El servicio no envía correos ni conoce canales de comunicación. La implementación de `StockAlertListener` define si la alerta se envía por correo u otros canales futuros.

## Antes de producción

- Guardar productos, reservas y el estado de las alertas en una base de datos compartida. Las reservas deben realizarse en una transacción que evite vender más stock del disponible.
- Guardar un identificador único por orden y su resultado para que los reintentos sean seguros. El vencimiento de reservas debe procesarse desde la base de datos y una sola instancia debe manejar cada reserva.
- Mover las reglas de categorías a configuración administrada o a una base de datos cuando sea necesario cambiarlas sin desplegar una nueva versión.
- Enviar alertas con eventos durables o un patrón outbox, para no perderlas si el proceso se detiene. Añadir monitoreo, autenticación, validación de entradas y métricas operativas.
