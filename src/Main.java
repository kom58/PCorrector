import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

void main() {
    SwingUtilities.invokeLater(this::seleccionarInforme);
}

void seleccionarInforme() {

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

    JFileChooser selectorInforme = new JFileChooser();
    selectorInforme.setDialogTitle(
            mensajeSeguro(
                    metodos,
                    "4040",
                    "Selecciona el informe que quieres abrir"
            )
    );
    selectorInforme.setFileSelectionMode(JFileChooser.FILES_ONLY);
    selectorInforme.setAcceptAllFileFilterUsed(false);
    selectorInforme.setFileFilter(
            new FileNameExtensionFilter(
                    mensajeSeguro(metodos, "4050", "Informes encriptados")
                            + " (*.lgx)",
                    "lgx"
            )
    );

    int resultado = selectorInforme.showOpenDialog(null);
    if (resultado != JFileChooser.APPROVE_OPTION) {
        return;
    }

    Path rutaInforme = selectorInforme.getSelectedFile().toPath();
    mostrarInforme(rutaInforme, metodos);
}

void mostrarInforme(Path rutaInforme, MetodosLb metodos) {
    JTextArea areaInforme = new JTextArea(
            mensajeSeguro(
                    metodos,
                    "4060",
                    "Abriendo el informe y generando la corrección..."
            )
    );
    areaInforme.setEditable(false);
    areaInforme.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
    areaInforme.setLineWrap(true);
    areaInforme.setWrapStyleWord(true);
    areaInforme.setMargin(new Insets(15, 15, 15, 15));

    JFrame ventanaInforme = new JFrame(
            mensajeSeguro(metodos, "4070", "Informe")
                    + " - " + rutaInforme.getFileName()
    );
    ventanaInforme.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    JScrollPane desplazamientoInforme = new JScrollPane(
            areaInforme,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    );
    ventanaInforme.add(desplazamientoInforme);
    ventanaInforme.setSize(800, 600);
    ventanaInforme.setLocationRelativeTo(null);
    ventanaInforme.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    ventanaInforme.setVisible(true);

    SwingWorker<String, Void> cargaInforme = new SwingWorker<>() {
        @Override
        protected String doInBackground() throws Exception {
            return new MetodosLb().leerInforme(rutaInforme);
        }

        @Override
        protected void done() {
            ventanaInforme.setCursor(Cursor.getDefaultCursor());

            try {
                areaInforme.setText(get());
                areaInforme.setCaretPosition(0);
            } catch (CancellationException e) {
                // La ventana se ha cerrado antes de terminar la carga.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                mostrarError(ventanaInforme, areaInforme,
                        mensajeSeguro(
                                metodos,
                                "4080",
                                "La apertura del informe se ha interrumpido"
                        ),
                        mensajeSeguro(
                                metodos,
                                "4100",
                                "Error al abrir el informe"
                        ));
            } catch (ExecutionException e) {
                Throwable causa = e.getCause();
                String mensaje = causa == null ? e.getMessage() : causa.getMessage();
                mostrarError(ventanaInforme, areaInforme,
                        mensajeSeguro(
                                metodos,
                                "4090",
                                "No se ha podido abrir el informe"
                        ) + ":\n" + mensaje,
                        mensajeSeguro(
                                metodos,
                                "4100",
                                "Error al abrir el informe"
                        ));
            }
        }
    };

    ventanaInforme.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
            cargaInforme.cancel(true);
        }
    });

    cargaInforme.execute();
}

void mostrarError(JFrame ventana, JTextArea areaInforme, String mensaje,
                  String titulo) {
    areaInforme.setText(mensaje);
    JOptionPane.showMessageDialog(
            ventana,
            mensaje,
            titulo,
            JOptionPane.ERROR_MESSAGE
    );
}

String mensajeSeguro(MetodosLb metodos, String codigo,
                     String mensajePredeterminado) {
    try {
        return metodos.leerMensajeIdioma(codigo);
    } catch (IOException | SecurityException e) {
        return mensajePredeterminado;
    }
}
