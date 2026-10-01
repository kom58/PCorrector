import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Calendar;
import java.util.regex.Pattern;

public class MetodosLb {

    private static final String NOMBRE_ARCHIVO_CONFIGURACION = "acr.ini";
    private static final Pattern PATRON_EMAIL = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );

    public Path rutaArchivoPcrIni() {
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
                    return carpetaJar.resolve(NOMBRE_ARCHIVO_CONFIGURACION);
                }
            }
        } catch (NullPointerException | SecurityException | URISyntaxException e) {
            // Si no se puede obtener la ubicacion del codigo, se usa el
            // directorio desde el que se ha iniciado la aplicacion.
        }

        return Path.of(System.getProperty("user.dir"))
                .toAbsolutePath()
                .normalize()
                .resolve(NOMBRE_ARCHIVO_CONFIGURACION);
    }

    public boolean crearArchivoPcrIni() throws IOException {
        String email = pedirEmailValido();
        if (email == null) {
            return false;
        }

        JComboBox<String> selectorIdioma = new JComboBox<>(
                new String[]{"Español", "Català"}
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
                + "idioma=" + idioma + System.lineSeparator();

        Path archivoAcrIni = rutaArchivoPcrIni();
        Files.writeString(
                archivoAcrIni,
                contenido,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
        return true;
    }

    public String leerArchivPcrIni() throws IOException {
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
            }
        }

        return contenido;
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
            throw new IOException("No se ha podido determinar la carpeta del informe.");
        }

        String nombreInforme = nombreArchivoSeguro(d.getUsuarioActual()) + ".lgx";
        final Path rutaFichero;
        try {
            rutaFichero = Path.of(d.getCarpetaFch()).resolve(nombreInforme);
        } catch (InvalidPathException e) {
            throw new IOException("La ruta del informe no es válida.", e);
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
        txt.append(ed.encripLin("FICHA    :    ", clave));
        txt.append(ed.encripLin(d.getNombreFch(), clave));
        txt.append(ed.encripLin("\n\n", clave));
        txt.append(ed.encripLin("Hora de inicio       : ", clave));
        txt.append(ed.encripLin(d.getHoraInicio(), clave));
        txt.append(ed.encripLin("\n", clave));
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

    private String nombreArchivoSeguro(String nombre) {
        String nombreSeguro = nombre == null ? "" : nombre.trim();

        // Caracteres no admitidos por Windows y caracteres de control.
        nombreSeguro = nombreSeguro.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
        // Windows tampoco permite que un nombre termine en un punto o espacio.
        nombreSeguro = nombreSeguro.replaceAll("[. ]+$", "");

        if (nombreSeguro.isBlank()) {
            nombreSeguro = "informe";
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

}
