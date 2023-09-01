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
    private DatagramSocket clientSocket;
    private InetAddress serverAddress;

    public Cliente(InetAddress serverAddress) {
        try {
            clientSocket = new DatagramSocket();
            this.serverAddress = serverAddress;
            System.out.println("Cliente conectado al servidor: " + serverAddress);
            mandarMensaje("me conecte");

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();
            privateKey = pair.getPrivate();
            publicKey = pair.getPublic();


        } catch (IOException | NoSuchAlgorithmException e) {
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
            String ipDestino = "/";
            String mensajeFinal = "";
            boolean arroba = false;

            for (int i=0; i < message.length(); i++){
                if (message.charAt(i) != '@' && !arroba){
                    ipDestino = ipDestino + message.charAt(i);


                }else {
                    arroba = true;
                    mensajeFinal = mensajeFinal + message.charAt(i);
                }
            }


            Mensaje mensaje =  new Mensaje(FirmaDigital.encrypt(ipDestino, Servidor.publicKey.toString()).toString(), FirmaDigital.encrypt(mensajeFinal, publicKey.toString()).toString(), FirmaDigital.hasheo(mensajeFinal));

            ByteArrayOutputStream bs= new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream (bs);
            os.writeObject(mensaje);
            os.close();

            byte[] sendBuffer =  bs.toByteArray();

            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, puertoDelServer);
            clientSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
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
                    FirmaDigital.decrypt(receiveBuffer, privateKey);

                    String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println("Mensaje recibido del servidor: " + receivedMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    throw new RuntimeException(e);
                } catch (IllegalBlockSizeException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (BadPaddingException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeyException e) {
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