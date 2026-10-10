import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class MetodosLb {

    private static final String NOMBRE_ARCHIVO_CONFIGURACION = "pcr.ini";
    private static final String NOMBRE_ARCHIVO_API = "pai.dt";
    private static final Pattern PATRON_EMAIL = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );
    private static final Pattern PATRON_NOMBRE_RESERVADO_WINDOWS = Pattern.compile(
            "(?i)^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?$"
    );
    private static final Pattern PATRON_CLAVE_PUBLICA = Pattern.compile(
            "[A-Za-z0-9]{4}"
    );
    private static final Pattern PATRON_LINEA_CIFRADA = Pattern.compile(
            "[A-Za-z0-9+/]+={0,2}"
    );
    private static final String PREFIJO_CRITERIO_ESCAPADO =
            "[PCorrector:CriterioEscapado]";
    private static final String VERSION_FORMATO_CRITERIOS = "Versión 1.0";
    private static final String CARACTERES_CLAVE_PUBLICA =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int LONGITUD_CLAVE_PUBLICA = 4;
    private static final SecureRandom GENERADOR_ALEATORIO = new SecureRandom();
    private static final int POSICION_NUMERO_FICHAS = 3;
    private static final int POSICION_PRIMERA_FICHA = 6;


    public String versionPCrr() {return "1.0.20";}

    public Path rutaArchivoPcrIni() {
        return rutaDirectorioConfiguracion().resolve(
                NOMBRE_ARCHIVO_CONFIGURACION
        );
    }

    public Path rutaArchivoPaiDt() {
        return rutaDirectorioConfiguracion().resolve(NOMBRE_ARCHIVO_API);
    }

    private Path rutaDirectorioConfiguracion() {
        Path directorioUsuario = rutaDirectorioUsuario();
        String sistemaOperativo = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT);

        if (sistemaOperativo.contains("mac")) {
            return directorioUsuario
                    .resolve("Library")
                    .resolve("Application Support")
                    .resolve("PCorrector");
        }

        if (sistemaOperativo.contains("win")) {
            Path appData = rutaVariableEntorno("APPDATA");
            if (appData == null) {
                appData = directorioUsuario
                        .resolve("AppData")
                        .resolve("Roaming");
            }
            return appData.resolve("PCorrector");
        }

        Path xdgConfigHome = rutaVariableEntorno("XDG_CONFIG_HOME");
        if (xdgConfigHome == null) {
            xdgConfigHome = directorioUsuario.resolve(".config");
        }
        return xdgConfigHome.resolve("PCorrector");
    }

    private Path rutaDirectorioUsuario() {
        try {
            String directorioUsuario = System.getProperty("user.home", "");
            if (!directorioUsuario.isBlank()) {
                return Path.of(directorioUsuario)
                        .toAbsolutePath()
                        .normalize();
            }
        } catch (InvalidPathException | SecurityException e) {
            // Si user.home no está disponible, se usa el directorio actual.
        }

        return Path.of(System.getProperty("user.dir", "."))
                .toAbsolutePath()
                .normalize();
    }

    private Path rutaVariableEntorno(String variable) {
        try {
            String valor = System.getenv(variable);
            if (valor != null && !valor.isBlank()) {
                Path ruta = Path.of(valor);
                if (ruta.isAbsolute()) {
                    return ruta.normalize();
                }
            }
        } catch (InvalidPathException | SecurityException e) {
            // Si la variable no contiene una ruta válida, se usa el fallback.
        }
        return null;
    }

    private Path rutaArchivoJuntoAplicacion(String nombreArchivo) {
        String carpetaConfigurada = System.getProperty("pcorrector.app.dir");
        if (carpetaConfigurada != null && !carpetaConfigurada.isBlank()) {
            try {
                return Path.of(carpetaConfigurada)
                        .toAbsolutePath()
                        .normalize()
                        .resolve(nombreArchivo);
            } catch (InvalidPathException | SecurityException e) {
                // Si la ruta indicada por el lanzador no es válida, se usa
                // la ubicación habitual de la aplicación.
            }
        }

        try {
            Path ubicacionAplicacion = Path.of(
                    MetodosLb.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).toAbsolutePath().normalize();

            // Al ejecutar un JAR, su carpeta es la carpeta de la aplicacion.
            if (Files.isRegularFile(ubicacionAplicacion)) {
                Path carpetaJar = ubicacionAplicacion.getParent();
                if (carpetaJar != null) {
                    return carpetaJar.resolve(nombreArchivo);
                }
            }
        } catch (NullPointerException | SecurityException | URISyntaxException e) {
            // Si no se puede obtener la ubicacion del codigo, se usa el
            // directorio desde el que se ha iniciado la aplicacion.
        }

        return Path.of(System.getProperty("user.dir"))
                .toAbsolutePath()
                .normalize()
                .resolve(nombreArchivo);
    }

    public boolean crearArchivoPcrIni() throws IOException {
        String email = pedirEmailValido();
        if (email == null) {
            return false;
        }

        JComboBox<String> selectorIdioma = new JComboBox<>(
                new String[]{
                        "Español",
                        "Català",
                        "Valencià",
                        "Galego",
                        "Euskara",
                        "Français",
                        "English"
                }
        );
        int resultado = JOptionPane.showConfirmDialog(
                null,
                new Object[]{"Selecciona el idioma:", selectorIdioma},
                "Idioma",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (resultado != JOptionPane.OK_OPTION) {
            return false;
        }

        String idioma = (String) selectorIdioma.getSelectedItem();
        String contenido = "email=" + email + System.lineSeparator()
                + "idioma=" + idioma + System.lineSeparator()
                + "ultimo=" + System.lineSeparator();

        Path archivoPcrIni = rutaArchivoPcrIni();
        Files.createDirectories(archivoPcrIni.getParent());
        Files.writeString(
                archivoPcrIni,
                contenido,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
        return true;
    }

    public String leerArchivoPcrIni() throws IOException {
        String contenido = Files.readString(
                rutaArchivoPcrIni(),
                StandardCharsets.UTF_8
        );

        Datos datos = new Datos();
        for (String linea : contenido.split("\\R")) {
            String[] propiedad = linea.split("=", 2);
            if (propiedad.length != 2) {
                continue;
            }

            String clave = propiedad[0].trim();
            String valor = propiedad[1].trim();
            if (clave.equals("email")) {
                datos.setEmailUsuario(valor);
            } else if (clave.equals("idioma")) {
                datos.setIdioma(valor);
            } else if (clave.equals("ultimo")) {
                datos.setUltimoUsuario(valor);
            }
        }

        return contenido;
    }

    public void guardarUltimoUsuario(String usuario) throws IOException {
        guardarPropiedadConfiguracion("ultimo", usuario);
        new Datos().setUltimoUsuario(usuario);
    }

    public void guardarIdioma(String idioma) throws IOException {
        guardarPropiedadConfiguracion("idioma", idioma);
    }

    private void guardarPropiedadConfiguracion(
            String clave,
            String valor
    ) throws IOException {
        Path archivoPcrIni = rutaArchivoPcrIni();
        List<String> lineas = Files.readAllLines(
                archivoPcrIni,
                StandardCharsets.UTF_8
        );
        boolean propiedadEncontrada = false;

        for (int i = 0; i < lineas.size(); i++) {
            String[] propiedad = lineas.get(i).split("=", 2);
            if (propiedad.length == 2
                    && propiedad[0].trim().equalsIgnoreCase(clave)) {
                lineas.set(i, clave + "=" + valor);
                propiedadEncontrada = true;
            }
        }

        if (!propiedadEncontrada) {
            lineas.add(clave + "=" + valor);
        }

        Files.write(
                archivoPcrIni,
                lineas,
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    public String leerMensajeIdioma(String codigo) throws IOException {
        String idioma = new Datos().getIdioma();
        String nombreArchivo = switch (idioma == null
                ? ""
                : idioma.trim().toLowerCase(Locale.ROOT)) {
            case "català", "valencià" -> "Català.lng";
            case "galego" -> "Galego.lng";
            case "euskara" -> "Euskara.lng";
            case "français" -> "Français.lng";
            case "english" -> "English.lng";
            default -> "Español.lng";
        };

        String contenido = leerContenidoIdioma(nombreArchivo);
        for (String linea : contenido.split("\\R")) {
            String[] mensaje = linea.split("=", 2);
            if (mensaje.length == 2 && mensaje[0].trim().equals(codigo)) {
                return mensaje[1].trim();
            }
        }

        throw new IOException(
                "No se encuentra el mensaje " + codigo + " en " + nombreArchivo
        );
    }

    private String leerContenidoIdioma(String nombreArchivo) throws IOException {
        Path juntoAplicacion = rutaArchivoJuntoAplicacion(nombreArchivo);
        if (Files.isRegularFile(juntoAplicacion)) {
            return Files.readString(juntoAplicacion, StandardCharsets.UTF_8);
        }

        Path directorioTrabajo = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath()
                .normalize();
        Path rutaDesarrollo = directorioTrabajo.resolve(nombreArchivo);
        if (Files.isRegularFile(rutaDesarrollo)) {
            return Files.readString(rutaDesarrollo, StandardCharsets.UTF_8);
        }

        rutaDesarrollo = directorioTrabajo.resolve("src").resolve(nombreArchivo);
        if (Files.isRegularFile(rutaDesarrollo)) {
            return Files.readString(rutaDesarrollo, StandardCharsets.UTF_8);
        }

        try (InputStream recurso = MetodosLb.class.getResourceAsStream(
                "/" + nombreArchivo
        )) {
            if (recurso != null) {
                return new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        throw new IOException("No se encuentra el archivo " + nombreArchivo);
    }

    private String pedirEmailValido() {
        while (true) {
            String email = JOptionPane.showInputDialog(
                    null,
                    "Introduce tu email:",
                    "Configuración inicial",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (email == null) {
                return null;
            }

            email = email.trim();
            if (PATRON_EMAIL.matcher(email).matches()) {
                return email;
            }

            JOptionPane.showMessageDialog(
                    null,
                    "Introduce un email válido.",
                    "Email no válido",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    /**
     * Lee y desencripta todos los informes almacenados en un fichero .lgx.
     *
     * El metodo es compatible con el formato generado por ACorrector
     * escribirInforme, que puede contener varios informes
     * concatenados en un mismo fichero.</p>
     *
     * rutaFichero ruta del fichero .lgx
     * @return el contenido en texto claro de todos los informes
     * IOException si el fichero no se puede leer o su formato no es valido
     */
    public String leerInforme(Path rutaFichero) throws IOException {
        if (rutaFichero == null) {
            throw new IllegalArgumentException(
                    mensajeSeguro("4110", "La ruta del informe no puede ser nula")
            );
        }

        String contenidoCifrado = Files.readString(
                rutaFichero,
                StandardCharsets.UTF_8
        );

        StringBuilder contenidoDesencriptado = new StringBuilder();
        EncripDecrip ed = new EncripDecrip();
        Datos datos = new Datos();
        int posicion = 0;

        while (posicion < contenidoCifrado.length()) {
            int finClave = contenidoCifrado.indexOf('\n', posicion);
            if (finClave < 0) {
                throw formatoInformeNoValido(posicion);
            }

            String clave = quitarRetornoCarro(
                    contenidoCifrado.substring(posicion, finClave)
            );
            if (!clave.matches("\\d{4}")) {
                throw formatoInformeNoValido(posicion);
            }
            posicion = finClave + 1;

            int finVersion = contenidoCifrado.indexOf('\n', posicion);
            if (finVersion < 0) {
                throw formatoInformeNoValido(posicion);
            }
            String versionCifrada = quitarRetornoCarro(
                    contenidoCifrado.substring(posicion, finVersion)
            );
            // La version forma parte del formato interno del fichero, pero no
            // debe mostrarse como contenido del informe.
            desencriptarBloque(ed, versionCifrada, clave, posicion);
            posicion = finVersion + 1;

            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "\n       *********************************\n\n", clave, ed,
                    contenidoDesencriptado);
            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "                  ", clave, ed, contenidoDesencriptado);
            posicion = agregarCampo(contenidoCifrado, posicion,
                    "\n\n", clave, ed, contenidoDesencriptado);
            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "\n\n", clave, ed, contenidoDesencriptado);
            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "                  ", clave, ed, contenidoDesencriptado);
            posicion = agregarCampo(contenidoCifrado, posicion,
                    "\n", clave, ed, contenidoDesencriptado);
            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "\n", clave, ed, contenidoDesencriptado);
            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "                     ", clave, ed, contenidoDesencriptado);
            posicion = agregarCampo(contenidoCifrado, posicion,
                    "\n", clave, ed, contenidoDesencriptado);
            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "\n", clave, ed, contenidoDesencriptado);
            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "\n            ***********************\n\n", clave, ed,
                    contenidoDesencriptado);
            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "FICHA    :    ", clave, ed, contenidoDesencriptado);
            StringBuilder nombreFichaDesencriptado = new StringBuilder();
            posicion = agregarCampo(contenidoCifrado, posicion,
                    "\n\n", clave, ed, nombreFichaDesencriptado);
            String nombreFicha = nombreFichaDesencriptado.toString();
            contenidoDesencriptado.append(nombreFicha);
            datos.setFchActiva(nombreFicha);

            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "\n\n", clave, ed, contenidoDesencriptado);
            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "Hora de inicio       : ", clave, ed, contenidoDesencriptado);
            posicion = agregarCampo(contenidoCifrado, posicion,
                    "\n", clave, ed, contenidoDesencriptado);
            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "\n", clave, ed, contenidoDesencriptado);
            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "Hora de finalización : ", clave, ed, contenidoDesencriptado);
            posicion = agregarCampo(contenidoCifrado, posicion,
                    "\n\n", clave, ed, contenidoDesencriptado);
            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "\n\n", clave, ed, contenidoDesencriptado);
            posicion = reemplazarBloqueFijo(contenidoCifrado, posicion,
                    "[[[ R ]]]", "\n", clave, ed, contenidoDesencriptado);
            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "\n\n", clave, ed, contenidoDesencriptado);

            StringBuilder respuestaDesencriptada = new StringBuilder();
            posicion = agregarCampo(contenidoCifrado, posicion,
                    "\n\n", clave, ed, respuestaDesencriptada);
            String respUsuario = respuestaDesencriptada.toString();
            datos.setRespUsuario(respUsuario);
            contenidoDesencriptado.append(respUsuario);

            String correccion = correccionChatGpt(respUsuario);

            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "\n\n", clave, ed, contenidoDesencriptado);
            contenidoDesencriptado.append("CORRECCIÓN:\n\n");
            if (correccion != null) {
                contenidoDesencriptado.append(correccion);
            }
            contenidoDesencriptado.append("\n\n");
            posicion = reemplazarBloqueFijo(contenidoCifrado, posicion,
                    "<=#©#=>", "\n", clave, ed, contenidoDesencriptado);
            posicion = agregarBloqueFijo(contenidoCifrado, posicion,
                    "\n\n", clave, ed, contenidoDesencriptado);
        }

        return contenidoDesencriptado.toString();
    }

    private int agregarBloqueFijo(String contenidoCifrado, int posicion,
                                  String textoClaro, String clave,
                                  EncripDecrip ed, StringBuilder resultado)
            throws IOException {
        return reemplazarBloqueFijo(contenidoCifrado, posicion, textoClaro,
                textoClaro, clave, ed, resultado);
    }

    private int reemplazarBloqueFijo(String contenidoCifrado, int posicion,
                                     String textoClaro, String reemplazo,
                                     String clave, EncripDecrip ed,
                                     StringBuilder resultado)
            throws IOException {
        String bloqueCifrado = ed.encripLin(textoClaro, clave);
        if (bloqueCifrado == null
                || !contenidoCifrado.startsWith(bloqueCifrado, posicion)) {
            throw formatoInformeNoValido(posicion);
        }

        resultado.append(reemplazo);
        return posicion + bloqueCifrado.length();
    }

    private int agregarCampo(String contenidoCifrado, int posicion,
                             String separadorSiguiente, String clave,
                             EncripDecrip ed, StringBuilder resultado)
            throws IOException {
        String separadorCifrado = ed.encripLin(separadorSiguiente, clave);
        if (separadorCifrado == null) {
            throw formatoInformeNoValido(posicion);
        }

        // Incluso una cadena vacia produce un bloque AES de 16 bytes, que
        // ocupa 24 caracteres en Base64. Empezar la busqueda despues de ese
        // minimo evita confundir el campo con el separador que lo sigue.
        int finCampo = contenidoCifrado.indexOf(
                separadorCifrado,
                posicion + 24
        );
        if (finCampo < 0) {
            throw formatoInformeNoValido(posicion);
        }

        String campoCifrado = contenidoCifrado.substring(posicion, finCampo);
        resultado.append(desencriptarBloque(ed, campoCifrado, clave, posicion));
        return finCampo;
    }

    private String desencriptarBloque(EncripDecrip ed, String bloqueCifrado,
                                      String clave, int posicion)
            throws IOException {
        String textoClaro = ed.desencripLin(bloqueCifrado, clave);
        if (textoClaro == null) {
            throw formatoInformeNoValido(posicion);
        }
        return textoClaro;
    }

    private String quitarRetornoCarro(String texto) {
        return texto.endsWith("\r")
                ? texto.substring(0, texto.length() - 1)
                : texto;
    }

    private IOException formatoInformeNoValido(int posicion) {
        return new IOException(
                mensajeSeguro(
                        "4210",
                        "El formato del informe encriptado no es válido cerca de la posición"
                ) + " " + posicion + "."
        );
    }

    private String nombreArchivoSeguro(String nombre) {
        String nombreSeguro = nombre == null ? "" : nombre.trim();

        // Caracteres no admitidos por Windows y caracteres de control.
        nombreSeguro = nombreSeguro.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
        // Windows tampoco permite que un nombre termine en un punto o espacio.
        nombreSeguro = nombreSeguro.replaceAll("[. ]+$", "");

        if (nombreSeguro.isBlank()) {
            nombreSeguro = mensajeSeguro("4070", "Informe")
                    .toLowerCase(Locale.ROOT);
        }

        // Nombres reservados por Windows, incluso cuando llevan extensión.
        if (nombreSeguro.matches("(?i)CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")) {
            nombreSeguro = "_" + nombreSeguro;
        }

        return nombreSeguro;
    }


    public String fechaActual() {

        String fechaAc;
        Calendar ahora = Calendar.getInstance();
        int diaA = ahora.get(Calendar.DAY_OF_MONTH);
        int mesA = ahora.get(Calendar.MONTH) + 1;
        int anoA = ahora.get(Calendar.YEAR);

        fechaAc = String.format("%02d.%02d.%04d", diaA, mesA, anoA);

        return fechaAc;
    }

    public String horaActual() {

        String horaAc;
        Calendar ahora = Calendar.getInstance();
        int horaA = ahora.get(Calendar.HOUR_OF_DAY);
        int minA = ahora.get(Calendar.MINUTE);

        horaAc = String.format("%02d:%02d", horaA, minA);

        return horaAc;
    }

    public String correccionChatGpt(String respUser) {
        //System.out.print(respUser);
        Datos d=new Datos();
        if (d.getChatGptAPI().isEmpty()){
            return mensajeSeguro("4220", "Sin acceso a IA !!")
                    + "\n"
                    + mensajeSeguro(
                            "4230",
                            "Comprobar la conexión a internet y los permisos API"
                    );
        } else if (d.sinCorreccion){

            String respuesta = mensajeSeguro(
                    "4240",
                    "Lectura de respuesta sin corregir"
            );
            return respuesta;

        } else {
            if (d.getFchCriteriosCorrec() == null
                    || d.getFchCriteriosCorrec().isBlank()) {
                String error = mensajeSeguro(
                        "1080",
                        "Primero debes crear o abrir un archivo de criterios "
                                + "de corrección."
                );
                JOptionPane.showMessageDialog(
                        null,
                        error + " (.cri)",
                        mensajeSeguro("1060", "Criterios"),
                        JOptionPane.WARNING_MESSAGE
                );
                return "ERROR: " + error;
            }

            List<String> nombresFicha = Datos.getNombreArchivoFch();
            List<String> criteriosFicha = Datos.getCriteriosCorreccionFch();
            String fichaActiva = d.getFchActiva();
            int indiceFicha = nombresFicha.indexOf(fichaActiva);

            if (indiceFicha <= 0 || indiceFicha >= criteriosFicha.size()) {
                return mensajeSeguro(
                        "5070",
                        "ERROR: No se encontraron criterios de corrección para la ficha activa: "
                )
                        + (fichaActiva == null ? "" : fichaActiva);
            }

            String criter = criteriosFicha.get(indiceFicha);
            String respuesta = ChatGPT.preguntar(
                    mensajeSeguro("5000", "Actúa como profesor.") + "\n\n"
                            + mensajeSeguro(
                                    "5010",
                                    "Evalúa de 0 a 10 la respuesta."
                            ) + "\n\n"
                            + mensajeSeguro(
                                    "5020",
                                    "Explica brevemente los errores encontrados."
                            ) + "\n\n"
                            + mensajeSeguro(
                                    "5030",
                                    "La explicación no debe superar las 200 palabras."
                            ) + "\n"
                            + mensajeSeguro("5040", "PREGUNTA")
                            + " : " + criter + "\n"
                            + mensajeSeguro("5050", "RESPUESTA")
                            + ": " + respUser
            );
            return respuesta;
        }
    }

    public String leerPcrIni() throws IOException {
        Path rutaPaiDt = rutaArchivoPaiDt();
        String claveApi = leerClaveApi(rutaPaiDt);

        new Datos().setChatGptAPI(claveApi);
        return claveApi;
    }

    public String leerClaveApi(Path rutaPaiDt) throws IOException {
        if (rutaPaiDt == null) {
            throw new IOException("La ruta de pai.dt no puede ser nula");
        }

        String claveApi;
        try (BufferedReader lector = Files.newBufferedReader(
                rutaPaiDt,
                StandardCharsets.UTF_8
        )) {
            claveApi = lector.readLine();
        }

        if (claveApi != null && claveApi.startsWith("\uFEFF")) {
            claveApi = claveApi.substring(1);
        }
        claveApi = claveApi == null ? "" : claveApi.trim();

        if (claveApi.isEmpty()) {
            throw new IOException(
                    mensajeSeguro("4300", "La primera línea de")
                            + " " + NOMBRE_ARCHIVO_API + " "
                            + mensajeSeguro("4301", "está vacía")
            );
        }

        return claveApi;
    }

    public void copiarArchivoPaiDt(Path origen) throws IOException {
        if (origen == null || !Files.isRegularFile(origen)) {
            throw new IOException("El archivo pai.dt seleccionado no es válido");
        }

        Path destino = rutaArchivoPaiDt();
        Path directorio = destino.getParent();
        Files.createDirectories(directorio);
        Path temporal = Files.createTempFile(directorio, "pai-", ".tmp");

        try {
            Files.copy(
                    origen,
                    temporal,
                    StandardCopyOption.REPLACE_EXISTING
            );
            try {
                Files.move(
                        temporal,
                        destino,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(
                        temporal,
                        destino,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private String mensajeSeguro(String codigo, String mensajePredeterminado) {
        try {
            return leerMensajeIdioma(codigo);
        } catch (IOException | SecurityException e) {
            return mensajePredeterminado;
        }
    }

    public String crearNuevoCriterio() {
        while (true) {
            String nombrePropuesto = JOptionPane.showInputDialog(
                    null,
                    mensajeSeguro(
                            "1070",
                            "Introduce el nombre del nuevo archivo de corrección"
                    ),
                    mensajeSeguro("1071", "Nuevo criterio de corrección"),
                    JOptionPane.QUESTION_MESSAGE
            );

            if (nombrePropuesto == null) {
                return "";
            }

            String nombreBase = nombrePropuesto;
            if (nombreBase.toLowerCase(Locale.ROOT).endsWith(".cri")) {
                nombreBase = nombreBase.substring(0, nombreBase.length() - 4);
            }

            String nombreArchivo = nombreBase + ".cri";
            if (!esNombreArchivoCompatible(nombreBase, nombreArchivo)) {
                JOptionPane.showMessageDialog(
                        null,
                        mensajeSeguro(
                                "1072",
                                "El nombre del archivo no es válido para Windows y macOS."
                        ),
                        mensajeSeguro("1004", "Nombre no válido"),
                        JOptionPane.WARNING_MESSAGE
                );
                continue;
            }

            Path rutaCriterio = rutaArchivoJuntoAplicacion(nombreArchivo);
            try {
                crearArchivoCriterioInicial(rutaCriterio);
                new Datos().setFchCriteriosCorrec(rutaCriterio.toString());
                return rutaCriterio.toString();
            } catch (FileAlreadyExistsException e) {
                JOptionPane.showMessageDialog(
                        null,
                        mensajeSeguro(
                                "1073",
                                "Ya existe un archivo con ese nombre."
                        ),
                        mensajeSeguro("1004", "Nombre no válido"),
                        JOptionPane.WARNING_MESSAGE
                );
            } catch (IOException | SecurityException e) {
                JOptionPane.showMessageDialog(
                        null,
                        mensajeSeguro(
                                "1074",
                                "No se pudo crear el archivo de corrección"
                        ) + ":\n" + e.getMessage(),
                        mensajeSeguro("1031", "Error"),
                        JOptionPane.ERROR_MESSAGE
                );
                return "";
            }
        }
    }

    private void crearArchivoCriterioInicial(Path rutaCriterio)
            throws IOException {
        String usuarioActual = new Datos().getUsuarioActual();
        Datos.inicializarNomFchCriterFch();
        Datos datos = new Datos();
        datos.setNumeroFichas(0);
        datos.setClvCriteriosCorrec("");
        Files.write(
                rutaCriterio,
                List.of(
                        usuarioActual == null ? "" : usuarioActual,
                        VERSION_FORMATO_CRITERIOS,
                        "",
                        String.valueOf(Datos.numeroFichas),
                        Datos.getNombreArchivoFch().get(0),
                        Datos.getCriteriosCorreccionFch().get(0)
                ),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
    }

    public int leerNumeroFichasCriterio(Path rutaCriterio) throws IOException {
        List<String> lineas = Files.readAllLines(
                rutaCriterio,
                StandardCharsets.UTF_8
        );
        return leerNumeroFichasCriterio(lineas);
    }

    private int leerNumeroFichasCriterio(List<String> lineas)
            throws IOException {
        validarFormatoCriterio(lineas);

        try {
            int numeroFichas = Integer.parseInt(
                    lineas.get(POSICION_NUMERO_FICHAS).trim()
            );
            if (numeroFichas < 0) {
                throw new NumberFormatException();
            }
            return numeroFichas;
        } catch (NumberFormatException e) {
            throw new IOException("El índice del archivo de criterios no es válido.", e);
        }
    }

    public void guardarFichaCriterio(int indice) throws IOException {
        Datos datos = new Datos();
        if (datos.getFchCriteriosCorrec() == null
                || datos.getFchCriteriosCorrec().isBlank()) {
            throw new IOException("No hay un archivo de criterios seleccionado.");
        }
        if (indice <= 0
                || indice >= Datos.getNombreArchivoFch().size()
                || indice >= Datos.getCriteriosCorreccionFch().size()) {
            throw new IOException("El índice de la ficha no es válido.");
        }

        Path rutaCriterio = Path.of(datos.getFchCriteriosCorrec());
        ArchivoCriteriosEdicion archivo = leerArchivoCriteriosParaEdicion(
                rutaCriterio
        );
        List<String> lineas = archivo.lineas();

        int numeroActual = leerNumeroFichasCriterio(lineas);
        int nuevoNumero = numeroActual + 1;
        if (indice != nuevoNumero) {
            throw new IOException("El índice de la ficha no es consecutivo.");
        }

        List<FichaCriterio> fichas = leerFichasCriterio(
                lineas,
                numeroActual
        );
        fichas.add(new FichaCriterio(
                Datos.getNombreArchivoFch().get(indice),
                Datos.getCriteriosCorreccionFch().get(indice)
        ));
        escribirFichasCriterio(
                rutaCriterio,
                lineas,
                fichas,
                archivo.clavePublica()
        );
        cargarFichasEnDatos(fichas);
        datos.setNumeroFichas(nuevoNumero);
    }

    public void modificarFichaCriterio(int indice) throws IOException {
        Datos datos = new Datos();
        if (datos.getFchCriteriosCorrec() == null
                || datos.getFchCriteriosCorrec().isBlank()) {
            throw new IOException("No hay un archivo de criterios seleccionado.");
        }
        if (indice <= 0
                || indice >= Datos.getCriteriosCorreccionFch().size()) {
            throw new IOException("El índice de la ficha no es válido.");
        }

        Path rutaCriterio = Path.of(datos.getFchCriteriosCorrec());
        ArchivoCriteriosEdicion archivo = leerArchivoCriteriosParaEdicion(
                rutaCriterio
        );
        List<String> lineas = archivo.lineas();
        int numeroFichas = leerNumeroFichasCriterio(lineas);
        if (indice > numeroFichas) {
            throw new IOException("El índice de la ficha no es válido.");
        }

        leerFichasCriterio(lineas, numeroFichas);
        int posicionCriterio = POSICION_PRIMERA_FICHA
                + (indice - 1) * 2 + 1;
        lineas.set(
                posicionCriterio,
                PREFIJO_CRITERIO_ESCAPADO
                        + escaparCriterio(
                                Datos.getCriteriosCorreccionFch().get(indice)
                        )
        );
        escribirArchivoCriteriosEditado(
                rutaCriterio,
                lineas,
                archivo.clavePublica()
        );
    }

    public void eliminarFichaCriterio(int indice) throws IOException {
        Datos datos = new Datos();
        if (datos.getFchCriteriosCorrec() == null
                || datos.getFchCriteriosCorrec().isBlank()) {
            throw new IOException("No hay un archivo de criterios seleccionado.");
        }

        Path rutaCriterio = Path.of(datos.getFchCriteriosCorrec());
        ArchivoCriteriosEdicion archivo = leerArchivoCriteriosParaEdicion(
                rutaCriterio
        );
        List<String> lineas = archivo.lineas();
        int numeroFichas = leerNumeroFichasCriterio(lineas);
        if (indice <= 0 || indice > numeroFichas) {
            throw new IOException("El índice de la ficha no es válido.");
        }

        List<FichaCriterio> fichas = leerFichasCriterio(
                lineas,
                numeroFichas
        );
        fichas.remove(indice - 1);
        escribirFichasCriterio(
                rutaCriterio,
                lineas,
                fichas,
                archivo.clavePublica()
        );
        cargarFichasEnDatos(fichas);
        datos.setNumeroFichas(fichas.size());
    }

    public List<String> cargarFichasCriterio(Path rutaCriterio)
            throws IOException {
        ArchivoCriteriosEdicion archivo = leerArchivoCriteriosParaEdicion(
                rutaCriterio
        );
        List<String> lineas = archivo.lineas();
        int numeroFichas = leerNumeroFichasCriterio(lineas);
        List<FichaCriterio> fichas = leerFichasCriterio(
                lineas,
                numeroFichas
        );
        new Datos().setClvCriteriosCorrec(lineas.get(2));

        cargarFichasEnDatos(fichas);
        List<String> nombres = new ArrayList<>();
        for (FichaCriterio ficha : fichas) {
            nombres.add(ficha.nombre());
        }
        new Datos().setNumeroFichas(numeroFichas);
        return nombres;
    }

    public String leerClaveCriterios(Path rutaCriterio) throws IOException {
        List<String> lineas = Files.readAllLines(
                rutaCriterio,
                StandardCharsets.UTF_8
        );
        validarFormatoCriterio(lineas);
        return lineas.get(2);
    }

    public boolean cifrarArchivoCriterios(
            Path rutaCriterio,
            String claveActual
    ) throws IOException {
        if (rutaCriterio == null) {
            throw new IOException("No hay un archivo de criterios seleccionado.");
        }

        Path rutaNormalizada = rutaCriterio.toAbsolutePath().normalize();
        String nombreArchivo = rutaNormalizada.getFileName() == null
                ? ""
                : rutaNormalizada.getFileName().toString();
        if (!nombreArchivo.toLowerCase(Locale.ROOT).endsWith(".cri")
                || !Files.isRegularFile(rutaNormalizada)) {
            throw new IOException("El archivo de criterios seleccionado no es válido.");
        }

        List<String> lineas = Files.readAllLines(
                rutaNormalizada,
                StandardCharsets.UTF_8
        );
        validarFormatoCriterio(lineas);
        String claveGuardada = lineas.get(2);
        if (claveGuardada.isEmpty() || !claveGuardada.equals(claveActual)) {
            new Datos().setClvCriteriosCorrec(claveGuardada);
            return false;
        }

        String clavePublica = crearClavePublica();
        EncripDecrip encriptador = new EncripDecrip();
        List<String> lineasCifradas = new ArrayList<>(lineas.size() + 1);
        lineasCifradas.add(clavePublica);
        for (String linea : lineas) {
            String lineaCifrada = encriptador.encripLin(linea, clavePublica);
            if (lineaCifrada == null) {
                throw new IOException(
                        "No se pudo cifrar el contenido del archivo de criterios."
                );
            }
            lineasCifradas.add(lineaCifrada);
        }

        escribirArchivoAtomico(rutaNormalizada, lineasCifradas);
        return true;
    }

    public String leerClaveCriteriosCifrado(Path rutaCriterio)
            throws IOException {
        List<String> lineasDescifradas = leerLineasCriteriosDescifradas(
                rutaCriterio
        );
        return lineasDescifradas == null
                ? null
                : lineasDescifradas.get(2);
    }

    public boolean cargarFichasCriterioCifrado(
            Path rutaCriterio,
            String claveActual
    ) throws IOException {
        List<String> lineasDescifradas = leerLineasCriteriosDescifradas(
                rutaCriterio
        );
        if (lineasDescifradas == null) {
            throw new IOException("El archivo de criterios no está cifrado.");
        }

        String claveGuardada = lineasDescifradas.get(2);
        if (!claveGuardada.isEmpty() && !claveGuardada.equals(claveActual)) {
            return false;
        }

        int numeroFichas = Integer.parseInt(
                lineasDescifradas.get(POSICION_NUMERO_FICHAS).trim()
        );
        List<FichaCriterio> fichas = leerFichasCriterio(
                lineasDescifradas,
                numeroFichas
        );
        cargarFichasEnDatos(fichas);
        Datos datos = new Datos();
        datos.setClvCriteriosCorrec(claveGuardada);
        datos.setNumeroFichas(numeroFichas);
        return true;
    }

    public boolean descifrarArchivoCriterios(
            Path rutaCriterio,
            String claveActual
    ) throws IOException {
        List<String> lineasDescifradas = leerLineasCriteriosDescifradas(
                rutaCriterio
        );
        if (lineasDescifradas == null) {
            throw new IOException("El archivo de criterios no está cifrado.");
        }

        String claveGuardada = lineasDescifradas.get(2);
        if (!claveGuardada.isEmpty() && !claveGuardada.equals(claveActual)) {
            return false;
        }

        Path rutaNormalizada = rutaCriterio.toAbsolutePath().normalize();
        escribirArchivoAtomico(rutaNormalizada, lineasDescifradas);
        new Datos().setClvCriteriosCorrec(claveGuardada);
        return true;
    }

    private List<String> leerLineasCriteriosDescifradas(Path rutaCriterio)
            throws IOException {
        if (rutaCriterio == null) {
            throw new IOException("No hay un archivo de criterios seleccionado.");
        }

        Path rutaNormalizada = rutaCriterio.toAbsolutePath().normalize();
        String nombreArchivo = rutaNormalizada.getFileName() == null
                ? ""
                : rutaNormalizada.getFileName().toString();
        if (!nombreArchivo.toLowerCase(Locale.ROOT).endsWith(".cri")
                || !Files.isRegularFile(rutaNormalizada)) {
            throw new IOException("El archivo de criterios seleccionado no es válido.");
        }

        List<String> lineasCifradas = Files.readAllLines(
                rutaNormalizada,
                StandardCharsets.UTF_8
        );
        if (lineasCifradas.size() < 2
                || !PATRON_CLAVE_PUBLICA.matcher(lineasCifradas.get(0)).matches()) {
            return null;
        }

        String clavePublica = lineasCifradas.get(0);
        EncripDecrip encriptador = new EncripDecrip();
        List<String> lineasDescifradas = new ArrayList<>(
                lineasCifradas.size() - 1
        );
        for (int indice = 1; indice < lineasCifradas.size(); indice++) {
            String lineaCifrada = lineasCifradas.get(indice);
            if (lineaCifrada.length() < 24
                    || !PATRON_LINEA_CIFRADA.matcher(lineaCifrada).matches()) {
                return null;
            }
            String lineaDescifrada = encriptador.desencripLin(
                    lineaCifrada,
                    clavePublica
            );
            if (lineaDescifrada == null) {
                return null;
            }
            lineasDescifradas.add(lineaDescifrada);
        }

        try {
            validarFormatoCriterio(lineasDescifradas);
            int numeroFichas = Integer.parseInt(
                    lineasDescifradas.get(POSICION_NUMERO_FICHAS).trim()
            );
            if (numeroFichas < 0) {
                return null;
            }
            leerFichasCriterio(lineasDescifradas, numeroFichas);
        } catch (IOException | NumberFormatException e) {
            return null;
        }
        return lineasDescifradas;
    }

    private String crearClavePublica() {
        StringBuilder clave = new StringBuilder(LONGITUD_CLAVE_PUBLICA);
        for (int indice = 0; indice < LONGITUD_CLAVE_PUBLICA; indice++) {
            clave.append(CARACTERES_CLAVE_PUBLICA.charAt(
                    GENERADOR_ALEATORIO.nextInt(CARACTERES_CLAVE_PUBLICA.length())
            ));
        }
        return clave.toString();
    }

    private void escribirArchivoAtomico(
            Path destino,
            List<String> lineas
    ) throws IOException {
        Path directorio = destino.getParent();
        if (directorio == null) {
            throw new IOException(
                    "No se pudo determinar la carpeta del archivo de criterios."
            );
        }

        Path temporal = Files.createTempFile(directorio, "criterios-", ".tmp");
        try {
            Files.write(
                    temporal,
                    lineas,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            try {
                Files.move(
                        temporal,
                        destino,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(
                        temporal,
                        destino,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    public boolean crearClaveCriterios(Path rutaCriterio, String clave)
            throws IOException {
        if (clave == null || clave.isEmpty()) {
            throw new IOException("La contraseña no puede estar vacía.");
        }

        List<String> lineas = Files.readAllLines(
                rutaCriterio,
                StandardCharsets.UTF_8
        );
        validarFormatoCriterio(lineas);
        String claveActual = lineas.get(2);
        if (!claveActual.isEmpty()) {
            new Datos().setClvCriteriosCorrec(claveActual);
            return false;
        }

        lineas.set(2, clave);
        Files.write(
                rutaCriterio,
                lineas,
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
        new Datos().setClvCriteriosCorrec(clave);
        return true;
    }

    public boolean modificarClaveCriterios(
            Path rutaCriterio,
            String claveActual,
            String claveNueva
    ) throws IOException {
        if (claveNueva == null || claveNueva.isEmpty()) {
            throw new IOException("La contraseña no puede estar vacía.");
        }

        List<String> lineas = Files.readAllLines(
                rutaCriterio,
                StandardCharsets.UTF_8
        );
        validarFormatoCriterio(lineas);
        String claveGuardada = lineas.get(2);
        if (claveGuardada.isEmpty() || !claveGuardada.equals(claveActual)) {
            new Datos().setClvCriteriosCorrec(claveGuardada);
            return false;
        }

        lineas.set(2, claveNueva);
        Files.write(
                rutaCriterio,
                lineas,
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
        new Datos().setClvCriteriosCorrec(claveNueva);
        return true;
    }

    public boolean eliminarClaveCriterios(
            Path rutaCriterio,
            String claveActual
    ) throws IOException {
        List<String> lineas = Files.readAllLines(
                rutaCriterio,
                StandardCharsets.UTF_8
        );
        validarFormatoCriterio(lineas);
        String claveGuardada = lineas.get(2);
        if (claveGuardada.isEmpty() || !claveGuardada.equals(claveActual)) {
            new Datos().setClvCriteriosCorrec(claveGuardada);
            return false;
        }

        lineas.set(2, "");
        Files.write(
                rutaCriterio,
                lineas,
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
        new Datos().setClvCriteriosCorrec("");
        return true;
    }

    private List<FichaCriterio> leerFichasCriterio(
            List<String> lineas,
            int numeroFichas
    ) throws IOException {
        validarFormatoCriterio(lineas);
        int lineasEsperadas = POSICION_PRIMERA_FICHA
                + numeroFichas * 2;
        if (lineas.size() != lineasEsperadas) {
            throw new IOException(
                    "El archivo de criterios no tiene el formato actual."
            );
        }

        List<FichaCriterio> fichas = new ArrayList<>();
        for (int indice = 0; indice < numeroFichas; indice++) {
            int posicionNombre = POSICION_PRIMERA_FICHA
                    + indice * 2;
            String nombre = lineas.get(posicionNombre);
            String criterioGuardado = lineas.get(posicionNombre + 1);
            String criterio = criterioGuardado.startsWith(
                    PREFIJO_CRITERIO_ESCAPADO
            )
                    ? desescaparCriterio(criterioGuardado.substring(
                            PREFIJO_CRITERIO_ESCAPADO.length()
                    ))
                    : criterioGuardado;
            fichas.add(new FichaCriterio(nombre, criterio));
        }
        return fichas;
    }

    private void escribirFichasCriterio(
            Path rutaCriterio,
            List<String> cabeceraOriginal,
            List<FichaCriterio> fichas,
            String clavePublica
    ) throws IOException {
        validarFormatoCriterio(cabeceraOriginal);
        List<String> lineas = new ArrayList<>(
                cabeceraOriginal.subList(0, POSICION_PRIMERA_FICHA)
        );
        lineas.set(
                POSICION_NUMERO_FICHAS,
                String.valueOf(fichas.size())
        );
        for (FichaCriterio ficha : fichas) {
            lineas.add(ficha.nombre());
            lineas.add(
                    PREFIJO_CRITERIO_ESCAPADO
                            + escaparCriterio(ficha.criterio())
            );
        }
        escribirArchivoCriteriosEditado(
                rutaCriterio,
                lineas,
                clavePublica
        );
    }

    private ArchivoCriteriosEdicion leerArchivoCriteriosParaEdicion(
            Path rutaCriterio
    ) throws IOException {
        List<String> lineasDescifradas = leerLineasCriteriosDescifradas(
                rutaCriterio
        );
        if (lineasDescifradas != null) {
            String claveGuardada = lineasDescifradas.get(2);
            String claveEnMemoria = new Datos().getClvCriteriosCorrec();
            if (!claveGuardada.isEmpty()
                    && !claveGuardada.equals(claveEnMemoria)) {
                throw new IOException(
                        "La contraseña del archivo de criterios no es válida."
                );
            }
            List<String> lineasCifradas = Files.readAllLines(
                    rutaCriterio,
                    StandardCharsets.UTF_8
            );
            return new ArchivoCriteriosEdicion(
                    lineasDescifradas,
                    lineasCifradas.get(0)
            );
        }

        List<String> lineas = Files.readAllLines(
                rutaCriterio,
                StandardCharsets.UTF_8
        );
        validarFormatoCriterio(lineas);
        return new ArchivoCriteriosEdicion(lineas, null);
    }

    private void escribirArchivoCriteriosEditado(
            Path rutaCriterio,
            List<String> lineas,
            String clavePublica
    ) throws IOException {
        if (clavePublica == null) {
            Files.write(
                    rutaCriterio,
                    lineas,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            return;
        }

        EncripDecrip encriptador = new EncripDecrip();
        List<String> lineasCifradas = new ArrayList<>(lineas.size() + 1);
        lineasCifradas.add(clavePublica);
        for (String linea : lineas) {
            String lineaCifrada = encriptador.encripLin(linea, clavePublica);
            if (lineaCifrada == null) {
                throw new IOException(
                        "No se pudo cifrar el contenido del archivo de criterios."
                );
            }
            lineasCifradas.add(lineaCifrada);
        }
        escribirArchivoAtomico(rutaCriterio, lineasCifradas);
    }

    private void validarFormatoCriterio(List<String> lineas)
            throws IOException {
        if (lineas.size() < POSICION_PRIMERA_FICHA) {
            throw new IOException(
                    "El archivo de criterios no contiene la cabecera completa."
            );
        }
        if (!VERSION_FORMATO_CRITERIOS.equals(lineas.get(1))) {
            throw new IOException(
                    "La versión del archivo de criterios no es compatible."
            );
        }
    }

    private void cargarFichasEnDatos(List<FichaCriterio> fichas) {
        Datos.inicializarNomFchCriterFch();
        for (FichaCriterio ficha : fichas) {
            Datos.getNombreArchivoFch().add(ficha.nombre());
            Datos.getCriteriosCorreccionFch().add(ficha.criterio());
        }
    }

    private String escaparCriterio(String criterio) {
        return criterio
                .replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String desescaparCriterio(String criterio) {
        StringBuilder resultado = new StringBuilder();
        boolean escapando = false;
        for (int posicion = 0; posicion < criterio.length(); posicion++) {
            char caracter = criterio.charAt(posicion);
            if (!escapando) {
                if (caracter == '\\') {
                    escapando = true;
                } else {
                    resultado.append(caracter);
                }
                continue;
            }

            switch (caracter) {
                case 'n' -> resultado.append('\n');
                case 'r' -> resultado.append('\r');
                case 't' -> resultado.append('\t');
                case '\\' -> resultado.append('\\');
                default -> resultado.append('\\').append(caracter);
            }
            escapando = false;
        }
        if (escapando) {
            resultado.append('\\');
        }
        return resultado.toString();
    }

    private record FichaCriterio(String nombre, String criterio) {
    }

    private record ArchivoCriteriosEdicion(
            List<String> lineas,
            String clavePublica
    ) {
    }

    private boolean esNombreArchivoCompatible(
            String nombreBase,
            String nombreArchivo
    ) {
        if (nombreBase.isEmpty()
                || !nombreBase.equals(nombreBase.strip())
                || nombreBase.equals(".")
                || nombreBase.equals("..")
                || nombreBase.endsWith(".")
                || nombreBase.endsWith(" ")
                || PATRON_NOMBRE_RESERVADO_WINDOWS.matcher(nombreBase).matches()
                || nombreArchivo.length() > 255
                || nombreArchivo.getBytes(StandardCharsets.UTF_8).length > 255) {
            return false;
        }

        String caracteresNoPermitidos = "<>:\"/\\|?*";
        for (int i = 0; i < nombreBase.length(); i++) {
            char caracter = nombreBase.charAt(i);
            if (Character.isISOControl(caracter)
                    || caracteresNoPermitidos.indexOf(caracter) >= 0) {
                return false;
            }
        }

        try {
            Path.of(nombreArchivo);
            return true;
        } catch (InvalidPathException e) {
            return false;
        }
    }

}
