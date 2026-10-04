
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Datos {
    public static String usuarioActual;
    public static String ultimoUsuario;
    public static String criteriosCorrecion;
    public static String clvCriteriosCorrec;
    public static boolean esAdmin;
    public static boolean sinCorreccion;
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
                                                                        // Criterios de corrección
    static List<String> nombreArchivoFch = new ArrayList<>();
    static List<String> criteriosCorreccionFch = new ArrayList<>();

    public void setUsuarioActual(String usuario){ usuarioActual = usuario;}
    public void setUltimoUsuario(String ultUsuario){ ultimoUsuario = ultUsuario;}
    public void setCriteriosCorreccion(String criteriosCorrec){ criteriosCorrecion = criteriosCorrec;}
    public void setClvCriteriosCorrec(String clvCriteriosCorrec){}
    public void setInforme(String infor){informe = infor;}
    public void setNombreFch(String nombre){nombreFch = nombre;}
    public void setEnvioEmailFch(String emailF) { envioEmailFch = emailF;}
    public void setEmailUsuario(String emailF) { emailUsuario = emailF;}
    public void setRespUsuario( String respF) {respUsuario = respF;}
    public void setHoraInicio(String horaIni) {horaInicio = horaIni;}
    public void setHoraFin(String horaF) { horaFin = horaF;}
    public void setIdioma(String idiom) {idioma = idiom;}
    public void setChatGptAPI(String chatGptA) {chatGptAPI = chatGptA;}
    public static void setNombreArchivoFch(List<String> nomArchivoFch) {nombreArchivoFch = nomArchivoFch;}
    public static void setCriteriosCorreccionFch(List<String> critCorreccionFch) {criteriosCorreccionFch = critCorreccionFch;}

    public void setArchivoInicialFch(String archIniF) {
        archivoInicialFch = archIniF;
        if (archIniF == null || archIniF.isEmpty()) { carpetaFch = ""; return;}
        Path rutaArchivo = Path.of(archIniF).toAbsolutePath().normalize();
        Path carpeta = rutaArchivo.getParent();
        carpetaFch = carpeta == null ? "" : carpeta.toString();
    }

    public String getUsuarioActual(){ return usuarioActual;}
    public String getUltimoUsuario(){ return ultimoUsuario;}
    public String getCriteriosCorrecion(){ return criteriosCorrecion;}
    public String getClvCriteriosCorrec(){ return clvCriteriosCorrec;}
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
    public static List<String> getNombreArchivoFch() {return nombreArchivoFch;}
    public static List<String> getCriteriosCorreccionFch() {return criteriosCorreccionFch;}


    public static void inicializar() {
        usuarioActual = "";
        ultimoUsuario = "";
        criteriosCorrecion = "";
        clvCriteriosCorrec = "";
        sinCorreccion = false;
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
        inicializarNomFchCriterFch();
    }

    public static void inicializarNomFchCriterFch(){
        nombreArchivoFch.clear();
        criteriosCorreccionFch.clear();

        nombreArchivoFch.add("[NombreFicha]");
        criteriosCorreccionFch.add("[CriteriosFicha]");
    }
}
