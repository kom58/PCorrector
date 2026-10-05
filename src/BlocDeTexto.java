//package corrector;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class BlocDeTexto extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final MetodosLb METODOS = new MetodosLb();
    private final JTextArea texto = new JTextArea();
    private final JFileChooser selector = new JFileChooser();
    private final JFileChooser selectorCriterios = new JFileChooser();
    private final JFileChooser selectorFichas = new JFileChooser();
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

        selectorCriterios.setDialogTitle(
                mensajeSeguro(
                        "1075",
                        "Selecciona un archivo de criterios de corrección"
                )
        );
        selectorCriterios.setFileSelectionMode(JFileChooser.FILES_ONLY);
        selectorCriterios.setAcceptAllFileFilterUsed(false);
        selectorCriterios.setFileFilter(
                new FileNameExtensionFilter(
                        mensajeSeguro("1076", "Archivos de criterios") + " (*.cri)",
                        "cri"
                )
        );

        selectorFichas.setDialogTitle(
                mensajeSeguro("1050", "Selecciona un archivo")
        );
        selectorFichas.setFileSelectionMode(JFileChooser.FILES_ONLY);
        selectorFichas.setAcceptAllFileFilterUsed(false);
        selectorFichas.setFileFilter(
                new FileNameExtensionFilter(
                        mensajeSeguro("1051", "Archivos")
                                + ": HTML, PDF, JPG, GIF, PNG",
                        "html",
                        "htm",
                        "pdf",
                        "jpg",
                        "gif",
                        "png"
                )
        );

        // Área de escritura
        texto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        add(new JScrollPane(texto), BorderLayout.CENTER);

        // Contador de palabras
        contador.setText(etiquetaPalabras + ": 0" + " " + Datos.fchCriteriosCorrec);
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
        JMenu ayuda = new JMenu(mensajeSeguro("1100", "Ayuda"));

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
        JMenuItem eliminarCriterio = new JMenuItem(
                mensajeSeguro("1065", "Eliminar")
        );

        nuevoCriterio.addActionListener(e -> METODOS.crearNuevoCriterio());
        abrirCriterio.addActionListener(e -> abrirArchivoCriterios());
        anadirCriterio.addActionListener(e -> anadirFichaCriterio());
        modificarCriterio.addActionListener(e -> seleccionarFichaParaModificar());
        eliminarCriterio.addActionListener(e -> seleccionarFichaParaEliminar());

        criterios.add(abrirCriterio);
        criterios.add(nuevoCriterio);
        fichas.add(anadirCriterio);
        fichas.add(modificarCriterio);
        fichas.add(eliminarCriterio);
        criterios.add(fichas);

        JMenuItem acercaDe = new JMenuItem(
                mensajeSeguro("1101", "Acerca de")
        );
        acercaDe.addActionListener(e -> mostrarAcercaDe());
        ayuda.add(acercaDe);

        barra.add(archivo);
        barra.add(criterios);
        barra.add(ayuda);
        setJMenuBar(barra);
    }

    private void mostrarAcercaDe() {
        JOptionPane.showMessageDialog(
                this,
                "PCorrector\n"
                        + mensajeSeguro("1102", "Versión") + ": "
                        + METODOS.versionPCrr(),
                mensajeSeguro("1101", "Acerca de"),
                JOptionPane.INFORMATION_MESSAGE
        );
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

        contador.setText(etiquetaPalabras + ": " + palabras + " " + Datos.fchCriteriosCorrec);
    }

    private void abrirArchivoCriterios() {
        if (selectorCriterios.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File archivo = selectorCriterios.getSelectedFile();
        try {
            String contenido = Files.readString(
                    archivo.toPath(),
                    StandardCharsets.UTF_8
            );
            METODOS.cargarFichasCriterio(archivo.toPath());
            new Datos().setFchCriteriosCorrec(
                    archivo.toPath().toAbsolutePath().normalize().toString()
            );
            texto.setText(contenido);
            texto.setCaretPosition(0);
        } catch (IOException | SecurityException e) {
            mostrarError(errorAbrirArchivo + ":\n" + e.getMessage());
        }
    }

    private void anadirFichaCriterio() {
        if (Datos.fchCriteriosCorrec == null
                || Datos.fchCriteriosCorrec.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    mensajeSeguro(
                            "1080",
                            "Primero debes crear o abrir un archivo de criterios "
                                    + "de corrección."
                    ),
                    mensajeSeguro("1060", "Criterios"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (selectorFichas.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File archivo = selectorFichas.getSelectedFile();
        if (!abrirEnNavegador(archivo)) {
            return;
        }

        int indice = new Datos().getNumeroFichas() + 1;
        while (Datos.getNombreArchivoFch().size() <= indice) {
            Datos.getNombreArchivoFch().add("");
        }
        while (Datos.getCriteriosCorreccionFch().size() <= indice) {
            Datos.getCriteriosCorreccionFch().add("");
        }

        Datos.getNombreArchivoFch().set(indice, archivo.getName());
        Datos.getCriteriosCorreccionFch().set(indice, "");
        mostrarEditorCriterios(indice);
    }

    private void seleccionarFichaParaModificar() {
        if (Datos.fchCriteriosCorrec == null
                || Datos.fchCriteriosCorrec.isBlank()) {
            mostrarAvisoSinArchivoCriterios();
            return;
        }

        final Path rutaCriterios;
        try {
            rutaCriterios = Path.of(Datos.fchCriteriosCorrec);
            if (!Files.isRegularFile(rutaCriterios)) {
                mostrarAvisoSinArchivoCriterios();
                return;
            }
        } catch (InvalidPathException | SecurityException e) {
            mostrarAvisoSinArchivoCriterios();
            return;
        }

        if (new Datos().getNumeroFichas() <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    mensajeSeguro(
                            "1086",
                            "El archivo de criterios no contiene fichas."
                    ),
                    mensajeSeguro("1060", "Criterios"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            java.util.List<String> nombres = METODOS.cargarFichasCriterio(
                    rutaCriterios
            );
            JComboBox<String> selectorFicha = new JComboBox<>(
                    nombres.toArray(String[]::new)
            );
            int resultado = JOptionPane.showConfirmDialog(
                    this,
                    selectorFicha,
                    mensajeSeguro("1062", "Modificar"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (resultado == JOptionPane.OK_OPTION
                    && selectorFicha.getSelectedIndex() >= 0) {
                mostrarEditorModificacionCriterios(
                        selectorFicha.getSelectedIndex() + 1
                );
            }
        } catch (IOException | SecurityException e) {
            mostrarError(errorAbrirArchivo + ":\n" + e.getMessage());
        }
    }

    private void seleccionarFichaParaEliminar() {
        if (Datos.fchCriteriosCorrec == null
                || Datos.fchCriteriosCorrec.isBlank()) {
            mostrarAvisoSinArchivoCriterios();
            return;
        }

        final Path rutaCriterios;
        try {
            rutaCriterios = Path.of(Datos.fchCriteriosCorrec);
            if (!Files.isRegularFile(rutaCriterios)) {
                mostrarAvisoSinArchivoCriterios();
                return;
            }
        } catch (InvalidPathException | SecurityException e) {
            mostrarAvisoSinArchivoCriterios();
            return;
        }

        if (new Datos().getNumeroFichas() <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    mensajeSeguro(
                            "1086",
                            "El archivo de criterios no contiene fichas."
                    ),
                    mensajeSeguro("1060", "Criterios"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            java.util.List<String> nombres = METODOS.cargarFichasCriterio(
                    rutaCriterios
            );
            JComboBox<String> selectorFicha = new JComboBox<>(
                    nombres.toArray(String[]::new)
            );
            int resultado = JOptionPane.showConfirmDialog(
                    this,
                    selectorFicha,
                    mensajeSeguro("1065", "Eliminar"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (resultado == JOptionPane.OK_OPTION
                    && selectorFicha.getSelectedIndex() >= 0) {
                confirmarEliminacionFicha(
                        selectorFicha.getSelectedIndex() + 1,
                        rutaCriterios
                );
            }
        } catch (IOException | SecurityException e) {
            mostrarError(errorAbrirArchivo + ":\n" + e.getMessage());
        }
    }

    private void confirmarEliminacionFicha(
            int indice,
            Path rutaCriterios
    ) {
        JTextArea contenidoCriterios = new JTextArea(
                Datos.getCriteriosCorreccionFch().get(indice),
                12,
                50
        );
        contenidoCriterios.setFont(
                new Font(Font.MONOSPACED, Font.PLAIN, 14)
        );
        contenidoCriterios.setLineWrap(true);
        contenidoCriterios.setWrapStyleWord(true);
        contenidoCriterios.setEditable(false);
        contenidoCriterios.setCaretPosition(0);

        String confirmar = mensajeSeguro("1089", "Confirmar");
        String cancelar = mensajeSeguro("1082", "Cancelar");
        int resultado = JOptionPane.showOptionDialog(
                this,
                new Object[]{
                        new JLabel(
                                mensajeSeguro(
                                        "1088",
                                        "¿Confirmas la eliminación de esta ficha?"
                                )
                        ),
                        new JScrollPane(contenidoCriterios)
                },
                Datos.getNombreArchivoFch().get(indice),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[]{confirmar, cancelar},
                cancelar
        );

        if (resultado != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            METODOS.eliminarFichaCriterio(indice);
            texto.setText(Files.readString(rutaCriterios, StandardCharsets.UTF_8));
            texto.setCaretPosition(0);
        } catch (IOException | SecurityException e) {
            mostrarError(
                    mensajeSeguro(
                            "1090",
                            "No se pudo eliminar la ficha"
                    ) + ":\n" + e.getMessage()
            );
        }
    }

    private void mostrarAvisoSinArchivoCriterios() {
        JOptionPane.showMessageDialog(
                this,
                mensajeSeguro(
                        "1080",
                        "Primero debes crear o abrir un archivo de criterios "
                                + "de corrección."
                ),
                mensajeSeguro("1060", "Criterios"),
                JOptionPane.WARNING_MESSAGE
        );
    }

    private boolean abrirEnNavegador(File archivo) {
        if (!Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            mostrarError(
                    mensajeSeguro(
                            "1030",
                            "No se puede abrir el navegador predeterminado"
                    )
            );
            return false;
        }

        try {
            Desktop.getDesktop().browse(archivo.toURI());
            return true;
        } catch (IOException | SecurityException e) {
            mostrarError(
                    mensajeSeguro(
                            "1040",
                            "No se pudo abrir el archivo en el navegador"
                    ) + ":\n" + e.getMessage()
            );
            return false;
        }
    }

    private void mostrarEditorCriterios(int indice) {
        JTextArea areaCriterios = new JTextArea(12, 50);
        areaCriterios.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        areaCriterios.setLineWrap(true);
        areaCriterios.setWrapStyleWord(true);

        JScrollPane desplazamiento = new JScrollPane(areaCriterios);
        String guardar = mensajeSeguro("1053", "Guardar");
        String cancelar = mensajeSeguro("1082", "Cancelar");
        int resultado = JOptionPane.showOptionDialog(
                this,
                new Object[]{
                        new JLabel(
                                mensajeSeguro(
                                        "1081",
                                        "Escribe detalladamente la pregunta y sus "
                                                + "criterios de corrección"
                                )
                        ),
                        desplazamiento
                },
                Datos.getNombreArchivoFch().get(indice),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{guardar, cancelar},
                guardar
        );

        if (resultado == JOptionPane.OK_OPTION) {
            Datos.getCriteriosCorreccionFch().set(
                    indice,
                    areaCriterios.getText()
            );
            try {
                METODOS.guardarFichaCriterio(indice);
                mostrarFichaCriterio(indice);
            } catch (IOException | SecurityException e) {
                mostrarError(
                        mensajeSeguro(
                                "1085",
                                "No se pudo guardar el archivo de criterios"
                        ) + ":\n" + e.getMessage()
                );
            }
        }
    }

    private void mostrarEditorModificacionCriterios(int indice) {
        JTextArea areaCriterios = new JTextArea(
                Datos.getCriteriosCorreccionFch().get(indice),
                12,
                50
        );
        areaCriterios.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        areaCriterios.setLineWrap(true);
        areaCriterios.setWrapStyleWord(true);
        areaCriterios.setCaretPosition(0);

        JScrollPane desplazamiento = new JScrollPane(areaCriterios);
        String guardar = mensajeSeguro("1053", "Guardar");
        String cancelar = mensajeSeguro("1082", "Cancelar");
        int resultado = JOptionPane.showOptionDialog(
                this,
                new Object[]{
                        new JLabel(
                                mensajeSeguro(
                                        "1087",
                                        "Ahora puedes modificar los criterios "
                                                + "de corrección"
                                )
                        ),
                        desplazamiento
                },
                Datos.getNombreArchivoFch().get(indice),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{guardar, cancelar},
                guardar
        );

        if (resultado == JOptionPane.OK_OPTION) {
            String criterioAnterior = Datos.getCriteriosCorreccionFch().get(
                    indice
            );
            Datos.getCriteriosCorreccionFch().set(
                    indice,
                    areaCriterios.getText()
            );
            try {
                METODOS.modificarFichaCriterio(indice);
                mostrarFichaCriterio(indice);
            } catch (IOException | SecurityException e) {
                Datos.getCriteriosCorreccionFch().set(
                        indice,
                        criterioAnterior
                );
                mostrarError(
                        mensajeSeguro(
                                "1085",
                                "No se pudo guardar el archivo de criterios"
                        ) + ":\n" + e.getMessage()
                );
            }
        }
    }

    private void mostrarFichaCriterio(int indice) {
        texto.setText(
                mensajeSeguro("1083", "Nombre de la ficha")
                        + ":\n"
                        + Datos.getNombreArchivoFch().get(indice)
                        + "\n\n"
                        + mensajeSeguro("1084", "Criterios de corrección")
                        + ":\n"
                        + Datos.getCriteriosCorreccionFch().get(indice)
        );
        texto.setCaretPosition(0);
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
