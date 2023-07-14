import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class Servidor {
    private static final int puertoServidor = 60246;
    private static final int tamañoDelBaffer = 1024;

    private DatagramSocket serverSocket;
    private Map<InetAddress, Integer> clients;


    public Servidor() {

        try {
            serverSocket = new DatagramSocket(puertoServidor);
            clients = new HashMap<>();
            System.out.println("Servidor listo para recibir conexiones...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        byte[] receiveBuffer = new byte[tamañoDelBaffer];
        while (true) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Mensaje recibido de " + clientAddress + ":" + clientPort + ": " + message);

                if (!clients.containsKey(clientAddress)) {
                    clients.put(clientAddress, clientPort);
                }

                // Verificar si el mensaje es un comando de envío de mensaje privado
                if (message.startsWith("@")) {

                    String[] parts = message.split(" ", 2);
                    if (parts.length == 2) {

                        String recipientName = parts[0].substring(1);
                        String privateMessage = parts[1];

                        InetAddress recipientAddress = null;
                        int recipientPort;

                        for (Map.Entry<InetAddress, Integer> entry : clients.entrySet()) {
                            if (entry.getValue().equals(clientPort)) {

                                recipientAddress = entry.getKey();
                                break;
                            }
                        }

                        if (recipientAddress != null) {
                            recipientPort = clients.get(recipientAddress);
                            sendMessageToClient("Mensaje privado de " + clientAddress + ":" + clientPort + ": " +
                            privateMessage, recipientAddress, recipientPort);

                        }
                    }
                }
                // Enviamos un ACK al cliente

                sendAck(clientAddress, clientPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void sendAck(InetAddress clientAddress, int clientPort) throws IOException {
        String ackMessage = "ACK";
        byte[] ackBuffer = ackMessage.getBytes();

        DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientAddress, clientPort);
        serverSocket.send(ackPacket);
    }
    private void sendMessageToClient(String message, InetAddress clientAddress, int clientPort) throws IOException {
        byte[] sendBuffer = message.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort);
        serverSocket.send(sendPacket);
    }


    public static void main(String[] args) {

        Servidor server = new Servidor();

        server.start();

    }

}