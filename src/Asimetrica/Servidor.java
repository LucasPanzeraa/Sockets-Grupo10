package src.src.Asimetrica;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.*;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("NonAsciiCharacters")
public class Servidor {
    private static final int puertoServidor = 42385;
    private static final int tamañoDelBaffer = 2048;
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

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                byte[] mensajeCifrado = receivePacket.getData();

                ByteArrayInputStream bais = new ByteArrayInputStream(mensajeCifrado);
                ObjectInputStream ois = new ObjectInputStream(bais);
                Mensaje mensaje = (Mensaje) ois.readObject();
                ois.close();


                if (!clients.containsKey(clientAddress)) {
                    clients.put(clientAddress, mensaje.getPubkey());
                    Mensaje mensaje1 = new Mensaje(null, null,null, publicKey);
                    sendMessageToClientPrueba(mensaje1,InetAddress.getByName(mensaje.getDestino()) ,clientPort);
                    System.out.println("mande el mensaje clave publica servidor");
                }
                else {

                    PublicKey publicaOrigen = null;
                    for (Map.Entry<InetAddress, PublicKey> cliente: clients.entrySet()){
                        if (cliente.getKey().equals(clientAddress)){
                            publicaOrigen = cliente.getValue();
                        }
                    }

                    InetAddress ipDestino = InetAddress.getByName(RSA.decryptWithPublic(mensaje.getDestino(), publicaOrigen));
                    System.out.println(ipDestino);

                    for (Map.Entry<InetAddress, PublicKey>clientes : clients.entrySet()){
                        if (clientes.getKey().equals(ipDestino)){

                            Mensaje mensajeDescifrado = descifrarMensaje(mensaje, publicaOrigen);

                            Mensaje mensajeFinal = cifrarMensaje(mensajeDescifrado, clientes.getValue());

                            sendMessageToClientPrueba(mensajeFinal,InetAddress.getByName(RSA.decryptWithPublic(mensaje.getDestino(), publicKey)) ,clientPort);

                            Mensaje mensaje1 = new Mensaje(null, null, null, null);
                            sendAck(mensaje1,clientAddress, clientPort);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void sendAck(Mensaje mensaje, InetAddress serverAddress, int puertoCliente) throws IOException {
        mensaje.setMensajeCifrado("ACK");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(mensaje);
        os.close();
        byte[] mensajeSerializado = baos.toByteArray();

        DatagramPacket sendPacket = new DatagramPacket(mensajeSerializado, mensajeSerializado.length, serverAddress, puertoCliente);

        serverSocket.send(sendPacket);
    }

    public Mensaje descifrarMensaje (Mensaje mensaje, PublicKey publicaOrigen) throws Exception {

        mensaje.setMensajeHasheado(RSA.decryptWithPublic(mensaje.getMensajeHasheado(), publicaOrigen));
        mensaje.setMensajeCifrado(RSA.decryptWithPrivate(mensaje.getMensajeCifrado(), privateKey));
        mensaje.setDestino(RSA.decryptWithPublic(mensaje.getDestino(), publicaOrigen));

        return mensaje;
    }

    public Mensaje cifrarMensaje(Mensaje mensaje, PublicKey publicaDestino) throws Exception {

        mensaje.setMensajeHasheado(RSA.encryptWithPrivate(mensaje.getMensajeHasheado(), privateKey));
        mensaje.setMensajeCifrado(RSA.encryptWithPublic(mensaje.getMensajeCifrado(), publicaDestino));
        mensaje.setDestino(RSA.encryptWithPrivate(mensaje.getDestino(), privateKey));

        return mensaje;
    }

    private void sendMessageToClientPrueba(Mensaje mensaje,InetAddress ipDestino , int puertoDestino) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(mensaje);
        os.close();

        byte[] mensajeSerializado = baos.toByteArray();

        // Crea un DatagramPacket con el mensaje cifrado
        DatagramPacket sendPacket = new DatagramPacket(mensajeSerializado, mensajeSerializado.length, ipDestino, puertoDestino);

        // Envía el paquete al servidor
        serverSocket.send(sendPacket);
    }
    public static void main(String[] args) {
        Servidor server = new Servidor();
        server.start();
    }
}
