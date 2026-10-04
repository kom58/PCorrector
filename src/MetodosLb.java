import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    public Path rutaArchivoPcrIni() {
        return rutaArchivoJuntoAplicacion(NOMBRE_ARCHIVO_CONFIGURACION);
    }

    private Path rutaArchivoJuntoAplicacion(String nombreArchivo) {
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


    public Path escribirInforme() throws IOException {

        Datos d = new Datos();

        if (d.getCarpetaFch() == null || d.getCarpetaFch().isBlank()) {
            throw new IOException(
                    mensajeSeguro(
                            "3000",
                            "No se ha podido determinar la carpeta del informe"
                    ) + "."
            );
        }

        String nombreInforme = nombreArchivoSeguro(d.getUsuarioActual()) + ".lgx";
        final Path rutaFichero;
        try {
            rutaFichero = Path.of(d.getCarpetaFch()).resolve(nombreInforme);
        } catch (InvalidPathException e) {
            throw new IOException(
                    mensajeSeguro("3010", "La ruta del informe no es válida") + ".",
                    e
            );
        }

        int clv = (int) (Math.random() * 8999 + 1000);                      // Clave pública
        String clave = String.valueOf(clv);

        StringBuilder txt = new StringBuilder();
        txt.append(clave + "\n");
                                                                            // Sin encriptar
        /*
        txt.append("\n       *********************************\n\n");
        txt.append("                  ").append(d.getUsuarioActual()).append("\n\n");
        txt.append("                  ").append(fechaActual()).append("\n");
        txt.append("                     ").append(horaActual()).append("\n");
        txt.append("\n            ***********************\n\n");
        txt.append("FICHA    :    ").append(d.getNombreFch()).append("\n\n");
        txt.append("Hora de inicio       : ").append(d.getHoraInicio()).append("\n");
        txt.append("Hora de finalización : ").append(d.getHoraFin()).append("\n\n");
        txt.append("[[[ R ]]]\n\n");
        txt.append(d.getRespUsuario()).append("\n\n");
        txt.append("<=#©#=>\n\n");
         */

                                                                            // Encriptado
        EncripDecrip ed = new EncripDecrip();
        txt.append(ed.encripLin("Versión 1.0", clave)).append("\n");
        txt.append(ed.encripLin("\n       *********************************\n\n", clave));
        txt.append(ed.encripLin("                  ", clave));
        txt.append(ed.encripLin(d.getUsuarioActual(),clave));
        txt.append(ed.encripLin("\n\n", clave));
        txt.append(ed.encripLin("                  ", clave));
        txt.append(ed.encripLin(fechaActual(), clave));
        txt.append(ed.encripLin("\n", clave));
        txt.append(ed.encripLin("                     ", clave));
        txt.append(ed.encripLin(horaActual(), clave));
        txt.append(ed.encripLin("\n", clave));
        txt.append(ed.encripLin("\n            ***********************\n\n", clave));
        //txt.append(ed.encripLin("FICHA    :    ", clave));
        txt.append(ed.encripLin("FICHA    :    ", clave));
        txt.append(ed.encripLin(d.getNombreFch(), clave));
        txt.append(ed.encripLin("\n\n", clave));
        //txt.append(ed.encripLin("Hora de inicio       : ", clave));
        txt.append(ed.encripLin("Hora de inicio       : ", clave));
        txt.append(ed.encripLin(d.getHoraInicio(), clave));
        txt.append(ed.encripLin("\n", clave));
        //txt.append(ed.encripLin("Hora de finalización : ", clave));
        txt.append(ed.encripLin("Hora de finalización : ", clave));
        txt.append(ed.encripLin(d.getHoraFin(), clave));
        txt.append(ed.encripLin("\n\n", clave));
        txt.append(ed.encripLin("[[[ R ]]]", clave));
        txt.append(ed.encripLin("\n\n", clave));
        txt.append(ed.encripLin(d.getRespUsuario(),clave));
        txt.append(ed.encripLin("\n\n", clave));
        txt.append(ed.encripLin("<=#©#=>", clave));
        txt.append(ed.encripLin("\n\n", clave));


        Files.writeString(
                rutaFichero,
                txt.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );

        return rutaFichero;
    }

    /**
     * Lee y desencripta todos los informes almacenados en un fichero .lgx.
     *
     * <p>El metodo es compatible con el formato generado por
     * {@link #escribirInforme()}, que puede contener varios informes
     * concatenados en un mismo fichero.</p>
     *
     * @param rutaFichero ruta del fichero .lgx
     * @return el contenido en texto claro de todos los informes
     * @throws IOException si el fichero no se puede leer o su formato no es valido
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
            posicion = agregarCampo(contenidoCifrado, posicion,
                    "\n\n", clave, ed, contenidoDesencriptado);
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

            String respuesta = ChatGPT.preguntar( "Actúa como profesor.\n" +
                                    "\n" +
                                    "        Evalúa de 0 a 10 la respuesta.\n" +
                                    "\n" +
                                    "        Explica brevemente los errores encontrados.\n" +
                                    "\n" +
                                    "        La explicación no debe superar las 200 palabras.\n" +
                                    " PREGUNTA : Describe el cuadro de Las Meninas de Velázquez\n" +
                                    " con un mínimo de 200 palabras\n" +
                                    "        RESPUESTA: " + respUser);


            return respuesta;
        }
    }

    public String leerPcrIni() throws IOException {
        Path rutaPcrIni = rutaArchivoJuntoAplicacion(NOMBRE_ARCHIVO_API);

        // Durante la ejecucion desde el IDE se admite tambien src/pai.dt.
        // Al ejecutar el JAR, el archivo debe estar junto a PCorrector.jar.
        if (!Files.isRegularFile(rutaPcrIni)) {
            Path rutaDesarrollo = Path.of(System.getProperty("user.dir"))
                    .toAbsolutePath()
                    .normalize()
                    .resolve("src")
                    .resolve(NOMBRE_ARCHIVO_API);
            if (Files.isRegularFile(rutaDesarrollo)) {
                rutaPcrIni = rutaDesarrollo;
            }
        }

        String claveApi;
        try (BufferedReader lector = Files.newBufferedReader(
                rutaPcrIni,
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

        new Datos().setChatGptAPI(claveApi);
        return claveApi;
    }

    private String mensajeSeguro(String codigo, String mensajePredeterminado) {
        try {
            return leerMensajeIdioma(codigo);
        } catch (IOException | SecurityException e) {
            return mensajePredeterminado;
        }
    }

}
