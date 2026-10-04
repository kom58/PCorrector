//package corrector;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class BlocDeTexto extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final MetodosLb METODOS = new MetodosLb();
    private final JTextArea texto = new JTextArea();
    private final JFileChooser selector = new JFileChooser();
    private final JLabel contador = new JLabel();
    private final JMenuItem abrir = new JMenuItem();
    private final JMenuItem sinCorregir = new JMenuItem();

    private final String etiquetaPalabras;
    private final String tituloError;
    private final String errorAbrirArchivo;
    private final String errorGuardarInforme;

    private final int numPreg;
    private final int totalPreg;

    public BlocDeTexto(int numPreg, int totalPreg) {
        this.numPreg = numPreg;
        this.totalPreg = totalPreg;

        etiquetaPalabras = mensajeSeguro("2001", "Palabras");
        tituloError = mensajeSeguro("1031", "Error");
        errorAbrirArchivo = mensajeSeguro("2020", "No se pudo abrir el archivo");
        errorGuardarInforme = mensajeSeguro("2030", "No se pudo guardar el informe");

        setTitle(mensajeSeguro("2010", "Respondiendo a") + ": " + Datos.nombreFch);
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        //setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);         // No deja cerrar !!!!
        setLocationRelativeTo(null);

        selector.setDialogTitle(
                mensajeSeguro("4040", "Selecciona el informe que quieres abrir")
        );
        selector.setFileSelectionMode(JFileChooser.FILES_ONLY);
        selector.setAcceptAllFileFilterUsed(false);
        selector.setFileFilter(
                new FileNameExtensionFilter(
                        mensajeSeguro("4050", "Informes encriptados") + " (*.lgx)",
                        "lgx"
                )
        );

        // Área de escritura
        texto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        add(new JScrollPane(texto), BorderLayout.CENTER);

        // Contador de palabras
        contador.setText(etiquetaPalabras + ": 0" + " / Aqui nombre de los criterios de corrección");
        contador.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(contador, BorderLayout.SOUTH);

        texto.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizarContador();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizarContador();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizarContador();
            }
        });

        // Menú Archivo
        JMenuBar barra = new JMenuBar();
        JMenu archivo = new JMenu(mensajeSeguro("1052", "Archivo"));
        JMenu criterios = new JMenu(mensajeSeguro("1060", "Criterios"));

        //JMenuItem nuevo = new JMenuItem("Nuevo");
        abrir.setText(mensajeSeguro("1054", "Abrir"));
        sinCorregir.setText(mensajeSeguro("1056", "Solo respuesta"));
        JMenuItem idioma = new JMenuItem(mensajeSeguro("1007", "Idioma"));
        JMenuItem salir = new JMenuItem(mensajeSeguro("1055", "Salir"));

        //nuevo.addActionListener(e -> texto.setText(""));
        abrir.addActionListener(e -> abrirArchivo(false));
        sinCorregir.addActionListener(e -> abrirArchivo(true));
        idioma.addActionListener(e -> mostrarSelectorIdioma());
        salir.addActionListener(e -> salirApp());

        //archivo.add(nuevo);
        archivo.add(abrir);
        archivo.add(sinCorregir);
        archivo.add(idioma);
        archivo.add(salir);

        JMenuItem abrirCriterio = new JMenuItem(mensajeSeguro("1054", "Abrir"));
        JMenuItem nuevoCriterio = new JMenuItem(mensajeSeguro("1061", "Nuevo"));
        JMenu fichas = new JMenu(mensajeSeguro("1064", "Fichas"));
        JMenuItem anadirCriterio = new JMenuItem(mensajeSeguro("1063", "Añadir"));
        JMenuItem modificarCriterio = new JMenuItem(
                mensajeSeguro("1062", "Modificar")
        );
        JMenuItem guardarCriterio = new JMenuItem(
                mensajeSeguro("1053", "Guardar")
        );

        criterios.add(abrirCriterio);
        criterios.add(nuevoCriterio);
        fichas.add(anadirCriterio);
        fichas.add(modificarCriterio);
        criterios.add(fichas);
        criterios.add(guardarCriterio);

        barra.add(archivo);
        barra.add(criterios);
        setJMenuBar(barra);
    }

    private void mostrarSelectorIdioma() {
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
        selectorIdioma.setSelectedItem(new Datos().getIdioma());

        int resultado = JOptionPane.showConfirmDialog(
                this,
                selectorIdioma,
                mensajeSeguro("1007", "Idioma"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (resultado != JOptionPane.OK_OPTION) {
            return;
        }

        String idioma = (String) selectorIdioma.getSelectedItem();
        if (idioma == null) {
            return;
        }
        if (idioma.equals(new Datos().getIdioma())) {
            return;
        }

        try {
            METODOS.guardarIdioma(idioma);
            JOptionPane.showMessageDialog(
                    this,
                    mensajeSeguro(
                            "1008",
                            "Se debe reiniciar el programa para que los cambios "
                                    + "de idioma tengan efecto"
                    ),
                    mensajeSeguro("1007", "Idioma"),
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException | SecurityException e) {
            JOptionPane.showMessageDialog(
                    this,
                    mensajeSeguro("1013", "No se pudo actualizar")
                            + " pcr.ini:\n" + e.getMessage(),
                    mensajeSeguro("1011", "Error de configuración"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void actualizarContador() {
        String contenido = texto.getText().strip();

        int palabras = contenido.isEmpty()
                ? 0
                : contenido.split("\\s+").length;

        contador.setText(etiquetaPalabras + ": " + palabras + " / Criterios");
    }

    private void abrirArchivo(boolean omitirCorreccion) {
        Datos.sinCorreccion = omitirCorreccion;
        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            cargarInforme(selector.getSelectedFile());
        }
    }

    public void cargarInforme(File archivo) {
        texto.setEditable(false);
        texto.setText(
                mensajeSeguro(
                        "4060",
                        "Abriendo el informe y generando la corrección..."
                )
        );
        texto.setCaretPosition(0);
        abrir.setEnabled(false);
        sinCorregir.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setTitle(
                mensajeSeguro("4070", "Informe") + " - " + archivo.getName()
        );

        SwingWorker<String, Void> cargaInforme = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return METODOS.leerInforme(archivo.toPath());
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                abrir.setEnabled(true);
                sinCorregir.setEnabled(true);
                texto.setEditable(true);

                try {
                    texto.setText(get());
                    texto.setCaretPosition(0);
                } catch (CancellationException e) {
                    // La carga se ha cancelado antes de terminar.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarErrorEnTexto(
                            mensajeSeguro(
                                    "4080",
                                    "La apertura del informe se ha interrumpido"
                            )
                    );
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    String mensaje = causa == null
                            ? e.getMessage()
                            : causa.getMessage();
                    mostrarErrorEnTexto(
                            mensajeSeguro(
                                    "4090",
                                    "No se ha podido abrir el informe"
                            ) + ":\n" + mensaje
                    );
                }
            }
        };

        cargaInforme.execute();
    }

    private void mostrarErrorEnTexto(String mensaje) {
        texto.setText(mensaje);
        texto.setCaretPosition(0);
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                mensajeSeguro("4100", "Error al abrir el informe"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void salirApp() {
        System.exit(0);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                tituloError,
                JOptionPane.ERROR_MESSAGE
        );
    }

    private String mensajeSeguro(String codigo, String mensajePredeterminado) {
        try {
            return METODOS.leerMensajeIdioma(codigo);
        } catch (IOException | SecurityException e) {
            return mensajePredeterminado;
        }
    }
}
