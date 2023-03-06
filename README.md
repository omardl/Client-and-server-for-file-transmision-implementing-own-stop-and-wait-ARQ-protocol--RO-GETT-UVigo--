# RO

Implementación de un protocolo ARQ para el control de errores en la comunicación entre un cliente y servidor para la transmisión de cualquier tipo de archivo de la manera más eficiente posible.

Para ello, se proporcionó por parte del profesorado un programa intermedio llamado "shuffle" que introducía de manera aleatoria distintos retardos en los paquetes y provocaba pérdidas de la misma manera, simulando así el comportamiento real de una red. Dicho programa también se encargaba de manera automática del cambio de IPs de los paquetes para el reenvío desde el "shuffle" al servidor o al cliente proporcionando total transparencia, teniendo que indicar al cliente únicamente en los primeros bytes de datos la IP y el puerto del servidor.

La transmisión del archivo es unidireccional de cliente a servidor.
