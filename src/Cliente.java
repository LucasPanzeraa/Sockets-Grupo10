package src.src;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.*;
import java.util.Scanner;

public class Cliente {
    private static final int puertoDelServer = 42385;
    private static final int TamañoDelBuffer = 1024;
    private static PrivateKey privateKey;
    private static PublicKey publicKey;
    private PublicKey  publicKeyServidor;
    private DatagramSocket clientSocket;
    private InetAddress serverAddress;

    public Cliente(InetAddress serverAddress) {
        try {
            clientSocket = new DatagramSocket();
            this.serverAddress = serverAddress;
            System.out.println("Cliente conectado al servidor: " + serverAddress);

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();
            privateKey = pair.getPrivate();
            publicKey = pair.getPublic();

            Mensaje mensaje = new Mensaje(null, null, null, publicKey);

            sendMessageToServer(mensaje);
            publicKeyServidor = recivirCalveDelServidor();
            System.out.println("clave " + publicKeyServidor);

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void inicio() {
        try {
            Thread receiveThread = new Thread(new recibirMensaje());
            receiveThread.start();

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Ingresar la ip del destinatario");
                String ipDestino = scanner.nextLine();

                System.out.println("ingrese el mensaje");
                String mensaje = scanner.nextLine();


                Mensaje message = encriptacionMensaje(ipDestino, mensaje);
                sendMessageToServer(message);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (clientSocket != null) {
                clientSocket.close();
            }
        }
    }

    public Mensaje encriptacionMensaje (String ipDestino, String mensaje) throws Exception {

        Mensaje mensaje1 = new Mensaje(RSA.encryptWithPrivate(RSA.hasheo(mensaje), privateKey), RSA.encryptWithPublic(mensaje, publicKeyServidor), ipDestino, null);

        return mensaje1;
    }

    public void sendMessageToServer(Mensaje mensaje) {
        try {
            // Serializa el mensaje
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(baos);
            os.writeObject(mensaje);
            os.close();
            byte[] mensajeSerializado = baos.toByteArray();

            if (publicKeyServidor != null){
                // Encripta el mensaje con la clave pública del servidor
                String mensajeCifrado = RSA.encryptWithPublic(mensajeSerializado.toString(), publicKeyServidor);

                byte[] mensajeCifradoByte = mensajeCifrado.getBytes();

                // Crea un DatagramPacket con el mensaje cifrado
                DatagramPacket sendPacket = new DatagramPacket(mensajeCifradoByte, mensajeCifradoByte.length, serverAddress, puertoDelServer);

                // Envía el paquete al servidor
                clientSocket.send(sendPacket);
            }
            else {
                byte[] mensajeByte = mensaje.toString().getBytes();
                DatagramPacket sendPacket = new DatagramPacket(mensajeByte, mensajeByte.length, serverAddress, puertoDelServer);
                clientSocket.send(sendPacket);
            }





        } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException |
                 InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public PublicKey recivirCalveDelServidor(){
        try {
            byte[] receiveBuffer = new byte[TamañoDelBuffer];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            // Espera a recibir un mensaje del servidor
            clientSocket.receive(receivePacket);

            // Convierte los datos recibidos en un String
            byte[] claveByte = new String( receivePacket.getData(), 0, receivePacket.getLength()).getBytes();

            PublicKey publicKeyServidor = RSA.getPublicKey(RSA.encode(claveByte));
        return publicKeyServidor;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    private class recibirMensaje implements Runnable {
        @Override
        public void run() {
            byte[] receiveBuffer = new byte[TamañoDelBuffer];

            while (true) {
                try {


                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    clientSocket.receive(receivePacket);
                    RSA.decryptWithPrivate(receiveBuffer.toString(), privateKey);

                    String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println("Mensaje recibido del servidor: " + receivedMessage);


                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                         InvalidKeyException | BadPaddingException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
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