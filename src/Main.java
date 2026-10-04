import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

void main() {
    SwingUtilities.invokeLater(this::iniciarAplicacion);
}

void iniciarAplicacion() {

    Datos.inicializar();
    MetodosLb metodos = new MetodosLb();

    Path archivoPcrIni = metodos.rutaArchivoPcrIni();
    try {
        if (Files.notExists(archivoPcrIni) && !metodos.crearArchivoPcrIni()) {
            return;
        }
    } catch (IOException | SecurityException e) {
        JOptionPane.showMessageDialog(
                null,
                mensajeSeguro(metodos, "1010", "No se pudo crear")
                        + " pcr.ini:\n" + e.getMessage(),
                mensajeSeguro(metodos, "1011", "Error de configuración"),
                JOptionPane.ERROR_MESSAGE
        );
        return;
    }

    try {
        metodos.leerArchivoPcrIni();
    } catch (IOException | SecurityException e) {
        JOptionPane.showMessageDialog(
                null,
                mensajeSeguro(metodos, "1012", "No se pudo leer")
                        + " pcr.ini:\n" + e.getMessage(),
                mensajeSeguro(metodos, "1011", "Error de configuración"),
                JOptionPane.ERROR_MESSAGE
        );
        return;
    }

    if (!pedirNombreUsuario(metodos)) {
        return;
    }

    try {
        metodos.leerPcrIni();
    } catch (IOException e) {
        JOptionPane.showMessageDialog(
                null,
                mensajeSeguro(metodos, "4010", "No se ha podido cargar")
                        + " pai.dt.\n"
                        + mensajeSeguro(
                                metodos,
                                "4020",
                                "La corrección con IA no estará disponible"
                        ) + ".\n\n"
                        + e.getMessage(),
                mensajeSeguro(metodos, "4030", "Configuración de la API"),
                JOptionPane.WARNING_MESSAGE
        );
    }

    BlocDeTexto blocDeTexto = new BlocDeTexto(1, 1);
    blocDeTexto.setVisible(true);
}

boolean pedirNombreUsuario(MetodosLb metodos) {
    String usuarioActual;

    do {
        usuarioActual = (String) JOptionPane.showInputDialog(
                null,
                mensajeSeguro(metodos, "1002", "Introduce tu nombre"),
                mensajeSeguro(metodos, "1001", "Inicio de sesión"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                new Datos().getUltimoUsuario()
        );

        if (usuarioActual == null) {
            return false;
        }

        usuarioActual = usuarioActual.trim();
        if (usuarioActual.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    mensajeSeguro(
                            metodos,
                            "1003",
                            "El nombre no puede estar vacío."
                    ),
                    mensajeSeguro(metodos, "1004", "Nombre no válido"),
                    JOptionPane.WARNING_MESSAGE
            );
        }
    } while (usuarioActual.isEmpty());

    try {
        metodos.guardarUltimoUsuario(usuarioActual);
    } catch (IOException | SecurityException e) {
        JOptionPane.showMessageDialog(
                null,
                mensajeSeguro(metodos, "1013", "No se pudo actualizar")
                        + " pcr.ini:\n" + e.getMessage(),
                mensajeSeguro(metodos, "1011", "Error de configuración"),
                JOptionPane.ERROR_MESSAGE
        );
        return false;
    }

    new Datos().setUsuarioActual(usuarioActual);
    JOptionPane.showMessageDialog(
            null,
            mensajeSeguro(metodos, "1005", "Bienvenido")
                    + ", " + usuarioActual + "!",
            mensajeSeguro(metodos, "1006", "Bienvenida"),
            JOptionPane.INFORMATION_MESSAGE
    );
    return true;
}

String mensajeSeguro(MetodosLb metodos, String codigo,
                     String mensajePredeterminado) {
    try {
        return metodos.leerMensajeIdioma(codigo);
    } catch (IOException | SecurityException e) {
        return mensajePredeterminado;
    }
}
