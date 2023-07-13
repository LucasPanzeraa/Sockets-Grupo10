package Sockets;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    private static final int puertoDelServidor = 12345;
    private static final int tamañoDelBuffer = 1024;

    public static void main(String[] args) {
        try {
            DatagramSocket socketCliente = new DatagramSocket(); // se crea el DatagramSocket para enviar y recibir datos sin establecer una conexion persistente
            InetAddress direccionServidor = InetAddress.getByName("localhost");

            System.out.println("Cliente UDP iniciado");

            while (true) {
                byte[] buffer = new byte[tamañoDelBuffer];

                Scanner scanner = new Scanner(System.in); // se  le pide al usuario que ingrese el nombre del detinatario y mensaje a enviar

                System.out.print("Ingrese el nombre del cliente destino: ");
                String destino = scanner.nextLine();

                System.out.print("Ingrese el mensaje: ");
                String mensaje = scanner.nextLine();

                String mensajeEnvio = destino + ": " + mensaje;
                byte[] bufferEnvio = mensajeEnvio.getBytes();  //se crea un buufer para pasar el mensaje a bytes

                DatagramPacket paqueteEnvio = new DatagramPacket(bufferEnvio, bufferEnvio.length, direccionServidor, puertoDelServidor);
                socketCliente.send(paqueteEnvio);   // se envia el mensaje al servidor con la dirección IP del servidor y el puerto definido.

                DatagramPacket paqueteRecibido = new DatagramPacket(buffer, buffer.length);
                socketCliente.receive(paqueteRecibido);  // se recibe la respuestra del servidor

                String mensajeRecibido = new String(paqueteRecibido.getData(), 0 , paqueteRecibido.getLength());  // offset es de donde se empiezan a leer los datos
                System.out.println("Respuesta del servidor: " + mensajeRecibido);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
