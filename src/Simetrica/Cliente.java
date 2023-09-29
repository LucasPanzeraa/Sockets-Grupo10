package src.src.Simetrica;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.*;
import java.util.Objects;
import java.util.Scanner;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@SuppressWarnings("NonAsciiCharacters")
public class Cliente {
    private static final int puertoDelServer = 42385;
    private static final int TamañoDelBuffer = 2048;
    private static PrivateKey privateKey;
    private static PublicKey publicKey;
    private PublicKey  publicKeyServidor;
    private SecretKey claveSecreta;
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
                String mensajeString = scanner.nextLine();

                Mensaje mensaje = crearMensaje(ipDestino, mensajeString);

                sendMessageToServer(mensaje);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (clientSocket != null) {
                clientSocket.close();
            }
        }
    }

    public Mensaje crearMensaje (String ipDestino, String mensaje){

        return new Mensaje(mensaje, mensaje, ipDestino, null);
    }

    public Mensaje encriptacionMensaje (Mensaje mensaje) throws Exception {

        return new Mensaje(RSA.encryptWithPrivate(RSA.hasheo(mensaje.getMensajeHasheado()), privateKey), RSA.encriptarConSecreta(mensaje.getMensajeCifrado(), claveSecreta), RSA.encryptWithPrivate(mensaje.getDestino(), privateKey), null );
    }

    public Mensaje desencriptarMensaje (Mensaje mensaje) throws Exception {
        mensaje.setMensajeHasheado(RSA.decryptWithPublic(mensaje.getMensajeHasheado(), publicKeyServidor));
        mensaje.setMensajeCifrado(RSA.desencriptarConSecreta(mensaje.getMensajeCifrado(), claveSecreta));

        return mensaje;
    }
    public Mensaje desencriptarClaveAES (Mensaje mensaje) throws Exception {
        mensaje.setMensajeHasheado(RSA.decryptWithPublic(mensaje.getMensajeHasheado(), publicKeyServidor));
        mensaje.setMensajeCifrado(RSA.decryptWithPrivate(mensaje.getMensajeCifrado(), privateKey));

        return mensaje;
    }

    public void comprobarIntegridad(Mensaje mensaje){
        if (RSA.hasheo(mensaje.getMensajeCifrado()).equals(mensaje.getMensajeHasheado())){
            System.out.println("El mensaje a llegado integro");
        }
        else {
            System.out.println("hubo una falla de integridad en el envio del mensaje");
        }
    }


    public static SecretKey base64SecretKey(String base64Key) {
        return new SecretKeySpec(Base64.getDecoder().decode(base64Key), 0, Base64.getDecoder().decode(base64Key).length, "AES");
    }


    public void sendMessageToServer(Mensaje mensaje) {
        try {


            if (publicKeyServidor != null){
                Mensaje mensajeCifrado = encriptacionMensaje(mensaje);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream os = new ObjectOutputStream(baos);
                os.writeObject(mensajeCifrado);
                os.close();
                byte[] mensajeSerializado = baos.toByteArray();

                DatagramPacket sendPacket = new DatagramPacket(mensajeSerializado, mensajeSerializado.length, serverAddress, puertoDelServer);

                clientSocket.send(sendPacket);
            }
            else{
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream os = new ObjectOutputStream(baos);
                os.writeObject(mensaje);
                os.close();
                byte[] mensajeSerializado = baos.toByteArray();

                DatagramPacket sendPacket = new DatagramPacket(mensajeSerializado, mensajeSerializado.length, serverAddress, puertoDelServer);

                clientSocket.send(sendPacket);
            }


        } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException |
                 InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private class recibirMensaje implements Runnable {

        public void run() {
            byte[] receiveBuffer = new byte[TamañoDelBuffer];

            while (true) {
                try {

                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    clientSocket.receive(receivePacket);

                    byte[] mensajeCifrado = receivePacket.getData();

                    ByteArrayInputStream bais = new ByteArrayInputStream(mensajeCifrado);
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Mensaje mensaje = (Mensaje) ois.readObject();
                    ois.close();

                    if (publicKeyServidor == null){
                        publicKeyServidor = mensaje.getPubkey();
                        Mensaje mensaje1 = desencriptarClaveAES(mensaje);
                        comprobarIntegridad(mensaje);
                        claveSecreta = base64SecretKey(mensaje1.getMensajeCifrado());
                    }
                    else {
                        if (!Objects.equals(mensaje.getMensajeCifrado(), "ACK")){
                            Mensaje mensajeDesencriptado = desencriptarMensaje(mensaje);
                            System.out.println("El mensaje recibido es: " + mensajeDesencriptado.getMensajeCifrado());
                            comprobarIntegridad(mensajeDesencriptado);
                        }
                        else {
                            System.out.println(mensaje.getMensajeCifrado());
                        }

                    }

                } catch (IOException e) {
                    e.printStackTrace();
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
