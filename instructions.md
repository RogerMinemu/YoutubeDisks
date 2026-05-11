# Youtube Disks Mod

El objetivo de desarrollar un mod de Minecraft 1.21.1 (neoforge-21.1.224) que permite grabar canciones de youtube en discos de minecraft.

## Convertir de video youtube a .ogg
El mod desde el cliente del usuario se conectará a youtube para descargar el video y localmente convertirlo en .ogg/mp3 para finalmente subirlo al servidor (todo esto a través de una GUI que podría tener un bloque llamado Disk Recorder, por ejemplo).

## Descargas de las canciones de los discos
Una vez el disco ha sido grabado, el .ogg será subido al servidor para posteriormente ser descargado por los clientes (así ya tenemos como una caché en el servidor, no hace falta volver a conectar a youtube y el cliente guardará una copia en local para cuando se vuelva a reproducir el mismo disco)

## Requisitos
- .jar buildeado para Minecraft 1.21.1 Modloader neoforge-21.1.224

## Texturas
Para los discos, disk recorder y otros items / modelos del mod, usa una carpeta de texturas y un png mock para que nuestra diseñadora haga la textura