import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
public class Servidor {
    private static final int puertoServidor = 42385;
    private static final int tamañoDelBaffer = 1024;
    private static PrivateKey privateKey;
    public static PublicKey publicKey;
    private DatagramSocket serverSocket;
    private Map<InetAddress, PublicKey> clients;
    public Servidor() {
        try {
            serverSocket = new DatagramSocket(puertoServidor);
            clients = new HashMap<>();
            System.out.println("Servidor listo para recibir conexiones...");

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();
            privateKey = pair.getPrivate();
            publicKey = pair.getPublic();

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    public void start() {
        byte[] receiveBuffer = new byte[tamañoDelBaffer];
        while (true) {
            try {

                ByteArrayInputStream bs= new ByteArrayInputStream(receiveBuffer); // bytes es el byte[
                ObjectInputStream is = new ObjectInputStream(bs);
                Mensaje mensaje = (Mensaje) is.readObject();
                is.close();

                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Mensaje recibido de " + clientAddress + ":" + clientPort + ": " + message);


                System.out.println(mensaje.getDestino());

                if (!clients.containsKey(clientAddress)) {
                    clients.put(clientAddress, mensaje.getPubkey());
                    sendMessageToClientPrueba(mensaje);
                }
                for (Map.Entry<InetAddress, PublicKey>clientes : clients.entrySet()){
                    if (clientes.getKey().toString().equals(FirmaDigital.decrypt(mensaje.getDestino().getBytes(),privateKey))){
                        InetAddress IPDestino = clientes.getKey();
                        sendMessageToClientPrueba(mensaje);

                        ByteArrayOutputStream mensajes = new ByteArrayOutputStream();
                        try {
                            ObjectOutputStream os = new ObjectOutputStream(mensajes);
                            os.writeObject(mensaje);
                            os.close();
                            byte[] mensajeSerializado = mensajes.toByteArray();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        serverSocket.send(receivePacket);
                        sendAck(clientAddress, clientPort);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                     BadPaddingException | IllegalBlockSizeException e) {
                throw new RuntimeException(e);
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

    private void sendMessageToClientPrueba(Mensaje mensaje) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        ByteArrayOutputStream bs= new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream (bs);
        os.writeObject(mensaje);
        os.close();
        byte[] sendBuffer =  bs.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length);
        serverSocket.send(sendPacket);
    }
    public static void main(String[] args) {
        Servidor server = new Servidor();
        server.start();
    }
}