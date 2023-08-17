import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
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

                String ipDestino = "/";
                String mensajeFinal = "";
                boolean arroba = false;

                for (int i=0; i < message.length(); i++){
                    if (message.charAt(i) != '@' && arroba == false){
                        ipDestino = ipDestino + message.charAt(i);


                    }else {
                        arroba = true;
                        mensajeFinal = mensajeFinal + message.charAt(i);
                    }
                }

                for (Map.Entry<InetAddress, Integer> clientes : clients.entrySet()){
                    InetAddress ipAux = clientes.getKey();
                    if (ipAux.toString().equals(ipDestino)){
                        byte[] mensaje = mensajeFinal.getBytes();
                        receivePacket = new DatagramPacket(mensaje, mensaje.length, ipAux, clientPort);
                        serverSocket.send(receivePacket);
                        sendAck(clientAddress, clientPort);

                    }
                }


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