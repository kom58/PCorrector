
import java.nio.file.Path;

public class Datos {
    public static String usuarioActual;
    public static boolean esAdmin;
    public static String administrador;
    public static String contrasena;
    public static String informe;
    public static String nombreFch;
    public static String archivoInicialFch;
    public static String envioEmailFch;
    public static String emailUsuario;
    public static String respUsuario;
    public static String carpetaFch;
    public static String horaInicio;
    public static String horaFin;
    public static String idioma;
    public static String chatGptAPI;


    public void setUsuarioActual(String usuario){ usuarioActual = usuario;}
    public void setInforme(String infor){informe = infor;}
    public void setNombreFch(String nombre){nombreFch = nombre;}
    public void setEnvioEmailFch(String emailF) { envioEmailFch = emailF;}
    public void setEmailUsuario(String emailF) { emailUsuario = emailF;}
    public void setRespUsuario( String respF) {respUsuario = respF;}
    public void setHoraInicio(String horaIni) {horaInicio = horaIni;}
    public void setHoraFin(String horaF) { horaFin = horaF;}
    public void setIdioma(String idiom) {idioma = idiom;}
    public void setChatGptAPI(String chatGptA) {chatGptAPI = chatGptA;}

    public void setArchivoInicialFch(String archIniF) {
        archivoInicialFch = archIniF;
        if (archIniF == null || archIniF.isEmpty()) { carpetaFch = ""; return;}
        Path rutaArchivo = Path.of(archIniF).toAbsolutePath().normalize();
        Path carpeta = rutaArchivo.getParent();
        carpetaFch = carpeta == null ? "" : carpeta.toString();
    }

    public String getUsuarioActual(){ return usuarioActual;}
    public String getInforme(){ return informe;}
    public String getNombreFch(){ return nombreFch;}
    public String getEnvioEmailFch(){ return envioEmailFch;}
    public String getEmailUsuario(){ return emailUsuario;}
    public String getRespUsuario(){ return respUsuario;}
    public String getArchivoInicialFch(){ return archivoInicialFch;}
    public String getCarpetaFch(){ return carpetaFch;}
    public String getHoraInicio(){ return horaInicio;}
    public String getHoraFin(){ return horaFin;}
    public String getIdioma(){ return idioma;}
    public String getChatGptAPI(){ return chatGptAPI;}



    public static void inicializar() {
        usuarioActual = "";
        informe = "";
        nombreFch = "";
        archivoInicialFch = "";
        envioEmailFch = "";
        emailUsuario = "";
        respUsuario = "";
        carpetaFch = "";
        horaInicio = "";
        horaFin = "";
        idioma = "";
        chatGptAPI = "";
    }
}
