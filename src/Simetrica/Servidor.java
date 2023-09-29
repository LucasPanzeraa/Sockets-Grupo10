package src.src.Simetrica;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("NonAsciiCharacters")
public class Servidor {
    private static final int puertoServidor = 42385;
    private static final int tamañoDelBaffer = 2048;
    private static PrivateKey privateKey;
    private static PublicKey publicKey;
    private static SecretKey secretKey = generarClaveAES();
    private DatagramSocket serverSocket;
    private Map<InetAddress, Integer> clientsPorts;
    private Map<InetAddress, PublicKey> clients;
    private Map<InetAddress, SecretKey> clientsSecretKey;
    public Servidor() {
        try {
            clientsSecretKey = new HashMap<>();
            serverSocket = new DatagramSocket(puertoServidor);
            clients = new HashMap<>();
            clientsPorts = new HashMap<>();
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
                    clientsSecretKey.put(clientAddress, secretKey);
                    clientsPorts.put(clientAddress, clientPort);
                    Mensaje mensaje1 = new Mensaje(RSA.encryptWithPrivate(RSA.hasheo(secretKeyBase64(secretKey)), privateKey), RSA.encryptWithPublic(secretKeyBase64(secretKey), mensaje.getPubkey()),null, publicKey);
                    sendMessageToClientPrueba(mensaje1,clientAddress ,clientPort);
                    System.out.println("mande el mensaje clave publica y secreta servidor");
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
                        System.out.println(clientes.getKey());
                    }

                    for (Map.Entry<InetAddress, PublicKey>clientes : clients.entrySet()){
                        if (clientes.getKey().equals(ipDestino)){

                            Mensaje mensajeDescifrado = descifrarMensaje(mensaje, publicaOrigen, clientsSecretKey.get(clientes.getKey()));

                            Mensaje mensajeFinal = cifrarMensaje(mensajeDescifrado, clientsSecretKey.get(clientes.getKey()));

                            sendMessageToClientPrueba(mensajeFinal,ipDestino ,clientsPorts.get(clientes.getKey()));

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
    private void sendAck(Mensaje mensaje, InetAddress ipOrigen, int puertoCliente) throws IOException {
        mensaje.setMensajeCifrado("ACK");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(mensaje);
        os.close();
        byte[] mensajeSerializado = baos.toByteArray();

        DatagramPacket sendPacket = new DatagramPacket(mensajeSerializado, mensajeSerializado.length, ipOrigen, puertoCliente);

        serverSocket.send(sendPacket);
    }

    public static SecretKey generarClaveAES() {
        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    public Mensaje descifrarMensaje (Mensaje mensaje, PublicKey publicaOrigen, SecretKey secretKey) throws Exception {

        mensaje.setMensajeHasheado(RSA.decryptWithPublic(mensaje.getMensajeHasheado(), publicaOrigen));
        mensaje.setMensajeCifrado(RSA.desencriptarConSecreta(mensaje.getMensajeCifrado(), secretKey));
        mensaje.setDestino(RSA.decryptWithPublic(mensaje.getDestino(), publicaOrigen));

        return mensaje;
    }

    public Mensaje cifrarMensaje(Mensaje mensaje, SecretKey secretKey) throws Exception {

        mensaje.setMensajeHasheado(RSA.encryptWithPrivate(mensaje.getMensajeHasheado(), privateKey));
        mensaje.setMensajeCifrado(RSA.encriptarConSecreta(mensaje.getMensajeCifrado(), secretKey));
        mensaje.setDestino(RSA.encryptWithPrivate(mensaje.getDestino(), privateKey));

        return mensaje;
    }


    public static String secretKeyBase64(SecretKey secretKey) {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    private void sendMessageToClientPrueba(Mensaje mensaje, InetAddress ipDestino , int puertoDestino) throws IOException {
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
