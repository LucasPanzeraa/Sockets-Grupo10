import java.io.Serializable;
import java.security.PublicKey;

public class Mensaje implements Serializable {
    private String mensajeHasheado;
    private String mensajeCifrado;
    private String destino;
    private PublicKey pubkey;
    public Mensaje(String mensajeHasheado, String mensajeCifrado, String destino, PublicKey pubkey) {
        this.mensajeHasheado = mensajeHasheado;
        this.mensajeCifrado = mensajeCifrado;
        this.destino = destino;
        this.pubkey = pubkey;
    }

    public PublicKey getPubkey() {
        return pubkey;
    }

    public void setPubkey(PublicKey pubkey) {
        this.pubkey = pubkey;
    }

    public String getMensajeHasheado() {
        return mensajeHasheado;
    }

    public void setMensajeHasheado(String mensajeHasheado) {
        this.mensajeHasheado = mensajeHasheado;
    }

    public String getMensajeCifrado() {
        return mensajeCifrado;
    }

    public void setMensajeCifrado(String mensajeCifrado) {
        this.mensajeCifrado = mensajeCifrado;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    @Override
    public String toString() {
        return  mensajeHasheado + mensajeCifrado + destino + pubkey;
    }
}
