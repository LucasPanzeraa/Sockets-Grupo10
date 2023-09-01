import java.io.Serializable;

public class Mensaje implements Serializable {
    private String mensajeHasheado;
    private String mensajeCifrado;
    private String destino;

    public Mensaje(String mensajeHasheado, String mensajeCifrado, String destino) {
        this.mensajeHasheado = mensajeHasheado;
        this.mensajeCifrado = mensajeCifrado;
        this.destino = destino;
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
}
