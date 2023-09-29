package src.src.ChatCliente_Servidor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class Cliente {
    private static final int puertoDelServer = 42385;
    private static final int TamañoDelBuffer = 1024;

    private DatagramSocket clientSocket;
    private InetAddress serverAddress;

    public Cliente(InetAddress serverAddress) {
        try {
            clientSocket = new DatagramSocket();
            this.serverAddress = serverAddress;
            System.out.println("Cliente conectado al servidor: " + serverAddress);
            mandarMensaje("me conecte");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void inicio() {
        try {
            Thread receiveThread = new Thread(new RecibirMensaje());
            receiveThread.start();

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Ingresar la ip y el mensaje a enviar");
                String message = scanner.nextLine();
                mandarMensaje(message);
            }
        } finally {
            if (clientSocket != null) {
                clientSocket.close();
            }
        }
    }

    private void mandarMensaje(String message) {
        try {
            byte[] sendBuffer = message.getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, puertoDelServer);
            clientSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class RecibirMensaje implements Runnable {
        @Override
        public void run() {
            byte[] receiveBuffer = new byte[TamañoDelBuffer];

            while (true) {
                try {

                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    clientSocket.receive(receivePacket);

                    String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println("Mensaje recibido del servidor: " + receivedMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            InetAddress serverAddress = InetAddress.getByName(scanner.nextLine());

            Cliente client = new Cliente(serverAddress);
            client.inicio();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
