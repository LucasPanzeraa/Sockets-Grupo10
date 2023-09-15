package src.src;

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
    private static PublicKey publicKey;
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

                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                byte[] mensajeCifrado = receivePacket.getData();
                byte[] mensajeDesencriptado = RSA.decryptWithPrivate(mensajeCifrado.toString(), privateKey).getBytes();

                ByteArrayInputStream bais = new ByteArrayInputStream(mensajeDesencriptado);
                ObjectInputStream ois = new ObjectInputStream(bais);
                Mensaje mensaje = (Mensaje) ois.readObject();
                ois.close();

                System.out.println(mensaje);

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                System.out.println("Mensaje recibido de " + clientAddress + ":" + clientPort + ": " + mensaje);
                System.out.println("Destino: " + mensaje.getDestino());
                System.out.println("Clave Pública del Cliente: " + mensajeDesencriptado.toString());

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Mensaje recibido de " + clientAddress + ":" + clientPort + ": " + message);


                System.out.println(mensaje.getDestino());

                if (!clients.containsKey(clientAddress)) {
                    clients.put(clientAddress, mensaje.getPubkey());
                    Mensaje mensaje1 = new Mensaje(null, null,null, publicKey);
                    sendMessageToClientPrueba(mensaje1);
                    System.out.println("mande el mensaje clave publica servidor");
                }
                for (Map.Entry<InetAddress, PublicKey>clientes : clients.entrySet()){
                    if (clientes.getKey().toString().equals(RSA.decryptWithPrivate(mensaje.getDestino(),privateKey))){
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
            } catch (Exception e) {
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(mensaje);
        os.close();

        byte[] mensajeSerializado = baos.toByteArray();

        // Crea un DatagramPacket con el mensaje cifrado
        DatagramPacket sendPacket = new DatagramPacket(mensajeSerializado, mensajeSerializado.length);

        // Envía el paquete al servidor
        serverSocket.send(sendPacket);
    }
    public static void main(String[] args) {
        Servidor server = new Servidor();
        server.start();
    }
}