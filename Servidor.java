package Sockets;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Servidor {
    private static final int puertoDelServidor = 12345;
    private static final int tamañoDelBuffer = 1024;

    public static void main(String[] args) {
        try {
            DatagramSocket socketServidor = new DatagramSocket(puertoDelServidor); // escucha en el puerto recibido

            System.out.println("Servidor UDP iniciado en el puerto " + puertoDelServidor);

            byte[] buffer = new byte[tamañoDelBuffer]; //  buffer para almacenar los datos recibidos.

            while (true) {
                DatagramPacket paqueteRecibido = new DatagramPacket(buffer, buffer.length); // recibe los mensajes enviados por los clientes
                socketServidor.receive(paqueteRecibido);

                InetAddress direccionCliente = paqueteRecibido.getAddress(); // se obtiene el puerto y la direccion ip para despues devolver el ACK
                int puertoCliente = paqueteRecibido.getPort();

                String mensajeRecibido = new String(paqueteRecibido.getData(), 0, paqueteRecibido.getLength());
                System.out.println("Mensaje recibido de " + direccionCliente + ":" + puertoCliente + ": " + mensajeRecibido);

                String[] partesMensaje = mensajeRecibido.split(":", 2); // dividimos el mensaje en dos por un lado el nombre del cliente de destino
                if (partesMensaje.length == 2) {                                   // y por el otro el contenido
                    String destino = partesMensaje[0].trim();
                    String contenidoMensaje = partesMensaje[1].trim();


                    InetAddress direccionDestino = buscarCliente(destino); // se crea un mensaje de ACK concatenando al contenido del mensaje.
                    if (direccionDestino != null) {
                        String mensajeAck = "ACK: " + contenidoMensaje;
                        byte[] bufferAck = mensajeAck.getBytes();   // se combierte el mensaje en un array de bytes para porder mandarlo

                        DatagramPacket paqueteAck = new DatagramPacket(bufferAck, bufferAck.length, direccionDestino, puertoCliente);
                        socketServidor.send(paqueteAck);        // se envia el mensaje con el puertop y la direccion ip del mensaje original

                    } else {
                        System.out.println("Cliente destino no encontrado: " + destino);
                    }
                } else {
                    System.out.println("Formato de mensaje incorrecto: " + mensajeRecibido);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // el nombreClinte va a ser la variable destino que pasemos en Cliente por lo qeu tenemos que configrurar ip y nombres por default
    private static InetAddress buscarCliente(String nombreCliente) { // ese metodo sirve para saber a que ip mandar el ACK

        if (nombreCliente.equalsIgnoreCase("Lucas")) {     // equalsIgnoreCase es para que se comparen dos strings
            try {                                                     // sin importar las mayusculas o las minusculas
                return InetAddress.getByName("192.168.0.1");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } else if (nombreCliente.equalsIgnoreCase("cliente2")) {
            try {
                return InetAddress.getByName("192.168.0.2");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        return null; // Si el cliente no se encuentra devuelve null
    }
}
