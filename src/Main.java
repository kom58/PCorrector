import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

void main() {
    SwingUtilities.invokeLater(this::seleccionarInforme);
}

void seleccionarInforme() {

    Datos.inicializar();

    try {
        new MetodosLb().leerPcrIni();
    } catch (IOException e) {
        JOptionPane.showMessageDialog(
                null,
                "No se ha podido cargar pcr.ini.\n"
                        + "La corrección con IA no estará disponible.\n\n"
                        + e.getMessage(),
                "Configuración de la API",
                JOptionPane.WARNING_MESSAGE
        );
    }

    JFileChooser selectorInforme = new JFileChooser();
    selectorInforme.setDialogTitle("Selecciona el informe que quieres abrir");
    selectorInforme.setFileSelectionMode(JFileChooser.FILES_ONLY);
    selectorInforme.setAcceptAllFileFilterUsed(false);
    selectorInforme.setFileFilter(
            new FileNameExtensionFilter("Informes encriptados (*.lgx)", "lgx")
    );

    int resultado = selectorInforme.showOpenDialog(null);
    if (resultado != JFileChooser.APPROVE_OPTION) {
        return;
    }

    Path rutaInforme = selectorInforme.getSelectedFile().toPath();
    mostrarInforme(rutaInforme);
}

void mostrarInforme(Path rutaInforme) {
    JTextArea areaInforme = new JTextArea(
            "Abriendo el informe y generando la corrección..."
    );
    areaInforme.setEditable(false);
    areaInforme.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
    areaInforme.setLineWrap(true);
    areaInforme.setWrapStyleWord(true);
    areaInforme.setMargin(new Insets(15, 15, 15, 15));

    JFrame ventanaInforme = new JFrame(
            "Informe - " + rutaInforme.getFileName()
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
                        "La apertura del informe se ha interrumpido.");
            } catch (ExecutionException e) {
                Throwable causa = e.getCause();
                String mensaje = causa == null ? e.getMessage() : causa.getMessage();
                mostrarError(ventanaInforme, areaInforme,
                        "No se ha podido abrir el informe:\n" + mensaje);
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

void mostrarError(JFrame ventana, JTextArea areaInforme, String mensaje) {
    areaInforme.setText(mensaje);
    JOptionPane.showMessageDialog(
            ventana,
            mensaje,
            "Error al abrir el informe",
            JOptionPane.ERROR_MESSAGE
    );
}
