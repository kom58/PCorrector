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
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class BlocDeTexto extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final boolean ES_MAC_OS = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .contains("mac");
    private static final MetodosLb METODOS = new MetodosLb();
    private final JTextArea texto = new JTextArea();
    private final JFileChooser selector = new JFileChooser();
    private final JFileChooser selectorCriterios = new JFileChooser();
    private final JFileChooser selectorFichas = new JFileChooser();
    private final JFileChooser selectorApi = new JFileChooser();
    private File ultimoDirectorio = obtenerDirectorioPersonal();
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

        selectorApi.setDialogTitle(
                mensajeSeguro("1140", "Selecciona el archivo pai.dt")
        );
        selectorApi.setFileSelectionMode(JFileChooser.FILES_ONLY);
        selectorApi.setAcceptAllFileFilterUsed(false);
        selectorApi.setFileFilter(
                new FileNameExtensionFilter(
                        mensajeSeguro(
                                "1141",
                                "Archivo de configuración de la API"
                        ) + " (pai.dt)",
                        "dt"
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
        JMenu configuracion = new JMenu(
                mensajeSeguro("1110", "Configuración")
        );
        JMenuItem copiarApi = new JMenuItem(
                mensajeSeguro("1111", "Copiar API")
        );
        JMenu contrasena = new JMenu(
                mensajeSeguro("1112", "Contraseña")
        );
        JMenuItem crearContrasena = new JMenuItem(
                mensajeSeguro("1113", "Crear")
        );
        JMenuItem modificarContrasena = new JMenuItem(
                mensajeSeguro("1062", "Modificar")
        );
        JMenuItem eliminarContrasena = new JMenuItem(
                mensajeSeguro("1065", "Eliminar")
        );

        nuevoCriterio.addActionListener(e -> METODOS.crearNuevoCriterio());
        abrirCriterio.addActionListener(e -> abrirArchivoCriterios());
        anadirCriterio.addActionListener(e -> anadirFichaCriterio());
        modificarCriterio.addActionListener(e -> seleccionarFichaParaModificar());
        eliminarCriterio.addActionListener(e -> seleccionarFichaParaEliminar());
        copiarApi.addActionListener(e -> copiarArchivoApi(copiarApi));
        crearContrasena.addActionListener(e -> crearContrasenaCriterios());
        modificarContrasena.addActionListener(e -> modificarContrasenaCriterios());
        eliminarContrasena.addActionListener(e -> eliminarContrasenaCriterios());

        criterios.add(abrirCriterio);
        criterios.add(nuevoCriterio);
        fichas.add(anadirCriterio);
        fichas.add(modificarCriterio);
        fichas.add(eliminarCriterio);
        criterios.add(fichas);
        configuracion.add(copiarApi);
        contrasena.add(crearContrasena);
        contrasena.add(modificarContrasena);
        contrasena.add(eliminarContrasena);
        configuracion.add(contrasena);
        criterios.add(configuracion);

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

    private void copiarArchivoApi(JMenuItem opcionCopiarApi) {
        File archivo = seleccionarArchivo(selectorApi, "dt");
        if (archivo == null) {
            return;
        }
        if (!archivo.getName().equalsIgnoreCase("pai.dt")) {
            JOptionPane.showMessageDialog(
                    this,
                    mensajeSeguro(
                            "1146",
                            "El archivo seleccionado debe llamarse pai.dt."
                    ),
                    mensajeSeguro("4030", "Configuración de la API"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JDialog progreso = crearDialogoProgresoApi();
        opcionCopiarApi.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<ChatGPT.ResultadoValidacionApi, Void> tarea =
                new SwingWorker<>() {
                    private String claveApi;

                    @Override
                    protected ChatGPT.ResultadoValidacionApi doInBackground()
                            throws Exception {
                        claveApi = METODOS.leerClaveApi(archivo.toPath());
                        ChatGPT.ResultadoValidacionApi resultado =
                                ChatGPT.validarApi(claveApi);
                        if (resultado.valida()) {
                            METODOS.copiarArchivoPaiDt(archivo.toPath());
                            new Datos().setChatGptAPI(claveApi);
                        }
                        return resultado;
                    }

                    @Override
                    protected void done() {
                        progreso.dispose();
                        opcionCopiarApi.setEnabled(true);
                        setCursor(Cursor.getDefaultCursor());

                        try {
                            ChatGPT.ResultadoValidacionApi resultado = get();
                            if (!resultado.valida()) {
                                String detalle = resultado.detalle();
                                mostrarError(
                                        mensajeSeguro(
                                                "1144",
                                                "La API seleccionada no es válida."
                                        )
                                                + (detalle == null
                                                || detalle.isBlank()
                                                ? ""
                                                : "\n" + detalle)
                                );
                                return;
                            }

                            JOptionPane.showMessageDialog(
                                    BlocDeTexto.this,
                                    mensajeSeguro(
                                            "1143",
                                            "La API es válida y el archivo pai.dt "
                                                    + "se ha copiado correctamente."
                                    ),
                                    mensajeSeguro(
                                            "4030",
                                            "Configuración de la API"
                                    ),
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            mostrarError(
                                    mensajeSeguro(
                                            "1145",
                                            "No se pudo copiar el archivo pai.dt."
                                    )
                            );
                        } catch (ExecutionException e) {
                            Throwable causa = e.getCause();
                            String detalle = causa == null
                                    ? e.getMessage()
                                    : causa.getMessage();
                            mostrarError(
                                    mensajeSeguro(
                                            "1145",
                                            "No se pudo copiar el archivo pai.dt."
                                    )
                                            + (detalle == null
                                            || detalle.isBlank()
                                            ? ""
                                            : "\n" + detalle)
                            );
                        }
                    }
                };

        progreso.setVisible(true);
        tarea.execute();
    }

    private JDialog crearDialogoProgresoApi() {
        JDialog dialogo = new JDialog(
                this,
                mensajeSeguro("4030", "Configuración de la API"),
                Dialog.ModalityType.MODELESS
        );
        JLabel mensaje = new JLabel(
                mensajeSeguro("1142", "Comprobando la API...")
        );
        mensaje.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        dialogo.add(mensaje);
        dialogo.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialogo.pack();
        dialogo.setResizable(false);
        dialogo.setLocationRelativeTo(this);
        return dialogo;
    }

    private void crearContrasenaCriterios() {
        Datos datos = new Datos();
        if (datos.getFchCriteriosCorrec() == null
                || datos.getFchCriteriosCorrec().isBlank()) {
            mostrarAvisoSinArchivoCriterios();
            return;
        }

        final Path rutaCriterios;
        try {
            rutaCriterios = Path.of(datos.getFchCriteriosCorrec());
            String claveActual = METODOS.leerClaveCriterios(rutaCriterios);
            if (!claveActual.isEmpty()) {
                mostrarAvisoContrasenaExistente();
                return;
            }
        } catch (IOException | InvalidPathException | SecurityException e) {
            mostrarError(errorAbrirArchivo + ":\n" + e.getMessage());
            return;
        }

        char[] primeraClave = pedirContrasena(
                mensajeSeguro("1121", "Introduce la contraseña")
        );
        if (primeraClave == null) {
            return;
        }
        if (primeraClave.length == 0) {
            Arrays.fill(primeraClave, '\0');
            JOptionPane.showMessageDialog(
                    this,
                    mensajeSeguro("1124", "La contraseña no puede estar vacía."),
                    mensajeSeguro("1112", "Contraseña"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        char[] segundaClave = pedirContrasena(
                mensajeSeguro("1122", "Repite la contraseña")
        );
        if (segundaClave == null) {
            Arrays.fill(primeraClave, '\0');
            return;
        }
        if (!Arrays.equals(primeraClave, segundaClave)) {
            Arrays.fill(primeraClave, '\0');
            Arrays.fill(segundaClave, '\0');
            JOptionPane.showMessageDialog(
                    this,
                    mensajeSeguro("1123", "Las contraseñas no coinciden."),
                    mensajeSeguro("1112", "Contraseña"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String clave = new String(primeraClave);
        Arrays.fill(primeraClave, '\0');
        Arrays.fill(segundaClave, '\0');
        try {
            if (!METODOS.crearClaveCriterios(rutaCriterios, clave)) {
                mostrarAvisoContrasenaExistente();
                return;
            }
            JOptionPane.showMessageDialog(
                    this,
                    mensajeSeguro("1125", "Contraseña creada correctamente."),
                    mensajeSeguro("1112", "Contraseña"),
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException | SecurityException e) {
            mostrarError(
                    mensajeSeguro(
                            "1085",
                            "No se pudo guardar el archivo de criterios"
                    ) + ":\n" + e.getMessage()
            );
        }
    }

    private void modificarContrasenaCriterios() {
        Datos datos = new Datos();
        if (datos.getFchCriteriosCorrec() == null
                || datos.getFchCriteriosCorrec().isBlank()) {
            mostrarAvisoSinArchivoCriterios();
            return;
        }

        final Path rutaCriterios;
        final String claveGuardada;
        try {
            rutaCriterios = Path.of(datos.getFchCriteriosCorrec());
            claveGuardada = METODOS.leerClaveCriterios(rutaCriterios);
            if (claveGuardada.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        mensajeSeguro(
                                "1128",
                                "El archivo de criterios no tiene contraseña. "
                                        + "Usa la opción Crear."
                        ),
                        mensajeSeguro("1112", "Contraseña"),
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        } catch (IOException | InvalidPathException | SecurityException e) {
            mostrarError(errorAbrirArchivo + ":\n" + e.getMessage());
            return;
        }

        char[] claveActual = pedirContrasena(
                mensajeSeguro("1129", "Introduce la contraseña actual")
        );
        if (claveActual == null) {
            return;
        }
        char[] claveEsperada = claveGuardada.toCharArray();
        boolean claveActualValida = Arrays.equals(claveActual, claveEsperada);
        Arrays.fill(claveActual, '\0');
        Arrays.fill(claveEsperada, '\0');
        if (!claveActualValida) {
            mostrarAvisoContrasenaIncorrecta();
            return;
        }

        char[] nuevaClave = pedirContrasena(
                mensajeSeguro("1130", "Introduce la nueva contraseña")
        );
        if (nuevaClave == null) {
            return;
        }
        if (nuevaClave.length == 0) {
            Arrays.fill(nuevaClave, '\0');
            JOptionPane.showMessageDialog(
                    this,
                    mensajeSeguro("1124", "La contraseña no puede estar vacía."),
                    mensajeSeguro("1112", "Contraseña"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        char[] confirmacion = pedirContrasena(
                mensajeSeguro("1131", "Repite la nueva contraseña")
        );
        if (confirmacion == null) {
            Arrays.fill(nuevaClave, '\0');
            return;
        }
        if (!Arrays.equals(nuevaClave, confirmacion)) {
            Arrays.fill(nuevaClave, '\0');
            Arrays.fill(confirmacion, '\0');
            JOptionPane.showMessageDialog(
                    this,
                    mensajeSeguro("1123", "Las contraseñas no coinciden."),
                    mensajeSeguro("1112", "Contraseña"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String nuevaClaveValidada = new String(nuevaClave);
        Arrays.fill(nuevaClave, '\0');
        Arrays.fill(confirmacion, '\0');
        try {
            if (!METODOS.modificarClaveCriterios(
                    rutaCriterios,
                    claveGuardada,
                    nuevaClaveValidada
            )) {
                mostrarAvisoContrasenaIncorrecta();
                return;
            }
            JOptionPane.showMessageDialog(
                    this,
                    mensajeSeguro(
                            "1132",
                            "Contraseña modificada correctamente."
                    ),
                    mensajeSeguro("1112", "Contraseña"),
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException | SecurityException e) {
            mostrarError(
                    mensajeSeguro(
                            "1085",
                            "No se pudo guardar el archivo de criterios"
                    ) + ":\n" + e.getMessage()
            );
        }
    }

    private void eliminarContrasenaCriterios() {
        Datos datos = new Datos();
        if (datos.getFchCriteriosCorrec() == null
                || datos.getFchCriteriosCorrec().isBlank()) {
            mostrarAvisoSinArchivoCriterios();
            return;
        }

        final Path rutaCriterios;
        final String claveGuardada;
        try {
            rutaCriterios = Path.of(datos.getFchCriteriosCorrec());
            claveGuardada = METODOS.leerClaveCriterios(rutaCriterios);
            if (claveGuardada.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        mensajeSeguro(
                                "1133",
                                "El archivo de criterios no tiene contraseña."
                        ),
                        mensajeSeguro("1112", "Contraseña"),
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        } catch (IOException | InvalidPathException | SecurityException e) {
            mostrarError(errorAbrirArchivo + ":\n" + e.getMessage());
            return;
        }

        char[] claveActual = pedirContrasena(
                mensajeSeguro("1129", "Introduce la contraseña actual")
        );
        if (claveActual == null) {
            return;
        }
        char[] claveEsperada = claveGuardada.toCharArray();
        boolean claveActualValida = Arrays.equals(claveActual, claveEsperada);
        Arrays.fill(claveActual, '\0');
        Arrays.fill(claveEsperada, '\0');
        if (!claveActualValida) {
            mostrarAvisoContrasenaIncorrecta();
            return;
        }

        try {
            if (!METODOS.eliminarClaveCriterios(
                    rutaCriterios,
                    claveGuardada
            )) {
                mostrarAvisoContrasenaIncorrecta();
                return;
            }
            JOptionPane.showMessageDialog(
                    this,
                    mensajeSeguro(
                            "1134",
                            "Contraseña eliminada correctamente."
                    ),
                    mensajeSeguro("1112", "Contraseña"),
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException | SecurityException e) {
            mostrarError(
                    mensajeSeguro(
                            "1085",
                            "No se pudo guardar el archivo de criterios"
                    ) + ":\n" + e.getMessage()
            );
        }
    }

    private char[] pedirContrasena(String mensaje) {
        JPasswordField campoContrasena = new JPasswordField(24);
        int resultado = JOptionPane.showConfirmDialog(
                this,
                new Object[]{new JLabel(mensaje), campoContrasena},
                mensajeSeguro("1112", "Contraseña"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        return resultado == JOptionPane.OK_OPTION
                ? campoContrasena.getPassword()
                : null;
    }

    private void mostrarAvisoContrasenaExistente() {
        JOptionPane.showMessageDialog(
                this,
                mensajeSeguro(
                        "1120",
                        "El archivo de criterios ya tiene una contraseña. "
                                + "Usa la opción Modificar."
                ),
                mensajeSeguro("1112", "Contraseña"),
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void mostrarAvisoContrasenaIncorrecta() {
        JOptionPane.showMessageDialog(
                this,
                mensajeSeguro("1127", "La contraseña es incorrecta."),
                mensajeSeguro("1112", "Contraseña"),
                JOptionPane.WARNING_MESSAGE
        );
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
        File archivo = seleccionarArchivo(selectorCriterios, "cri");
        if (archivo == null) {
            return;
        }

        Path rutaCriterios = archivo.toPath();
        try {
            if (!autorizarAperturaCriterios(rutaCriterios)) {
                return;
            }
            METODOS.cargarFichasCriterio(rutaCriterios);
            new Datos().setFchCriteriosCorrec(
                    rutaCriterios.toAbsolutePath().normalize().toString()
            );
            texto.setText(leerContenidoCriteriosVisible(rutaCriterios));
            texto.setCaretPosition(0);
        } catch (IOException | SecurityException e) {
            mostrarError(errorAbrirArchivo + ":\n" + e.getMessage());
        }
    }

    private boolean autorizarAperturaCriterios(Path rutaCriterios)
            throws IOException {
        String claveGuardada = METODOS.leerClaveCriterios(rutaCriterios);
        if (claveGuardada.isEmpty()) {
            return true;
        }

        char[] claveIntroducida = pedirContrasena(
                mensajeSeguro(
                        "1126",
                        "Introduce la contraseña del archivo de criterios"
                )
        );
        if (claveIntroducida == null) {
            return false;
        }

        char[] claveEsperada = claveGuardada.toCharArray();
        boolean coincide = Arrays.equals(claveIntroducida, claveEsperada);
        Arrays.fill(claveIntroducida, '\0');
        Arrays.fill(claveEsperada, '\0');
        if (!coincide) {
            mostrarAvisoContrasenaIncorrecta();
        }
        return coincide;
    }

    private static String leerContenidoCriteriosVisible(Path rutaCriterios)
            throws IOException {
        java.util.List<String> lineas = Files.readAllLines(
                rutaCriterios,
                StandardCharsets.UTF_8
        );
        if (lineas.size() < 3) {
            throw new IOException(
                    "El archivo de criterios no contiene la cabecera completa."
            );
        }

        StringBuilder contenido = new StringBuilder();
        for (int indice = 0; indice < lineas.size(); indice++) {
            if (indice == 1 || indice == 2) {
                continue;
            }
            if (!contenido.isEmpty()) {
                contenido.append(System.lineSeparator());
            }
            contenido.append(lineas.get(indice));
        }
        return contenido.toString();
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

        File archivo = seleccionarArchivo(
                selectorFichas,
                "html",
                "htm",
                "pdf",
                "jpg",
                "gif",
                "png"
        );
        if (archivo == null) {
            return;
        }

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
            texto.setText(leerContenidoCriteriosVisible(rutaCriterios));
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
        File archivo = seleccionarArchivo(selector, "lgx");
        if (archivo != null) {
            cargarInforme(archivo);
        }
    }

    private File seleccionarArchivo(
            JFileChooser selectorSwing,
            String... extensiones
    ) {
        File archivo;
        if (ES_MAC_OS) {
            archivo = seleccionarArchivoNativo(
                    selectorSwing.getDialogTitle(),
                    extensiones
            );
        } else {
            if (ultimoDirectorio.isDirectory()) {
                selectorSwing.setCurrentDirectory(ultimoDirectorio);
            }
            if (selectorSwing.showOpenDialog(this)
                    != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            archivo = selectorSwing.getSelectedFile();
        }

        if (archivo != null) {
            File directorio = archivo.getParentFile();
            if (directorio != null && directorio.isDirectory()) {
                ultimoDirectorio = directorio;
            }
        }
        return archivo;
    }

    private File seleccionarArchivoNativo(
            String titulo,
            String... extensiones
    ) {
        FileDialog dialogo = new FileDialog(this, titulo, FileDialog.LOAD);
        dialogo.setMultipleMode(false);
        if (ultimoDirectorio.isDirectory()) {
            dialogo.setDirectory(ultimoDirectorio.getAbsolutePath());
        }
        dialogo.setFilenameFilter(
                (directorio, nombre) -> tieneExtensionPermitida(
                        nombre,
                        extensiones
                )
        );
        dialogo.setVisible(true);

        String nombre = dialogo.getFile();
        String directorio = dialogo.getDirectory();
        dialogo.dispose();
        if (nombre == null) {
            return null;
        }
        return directorio == null
                ? new File(nombre)
                : new File(directorio, nombre);
    }

    private static boolean tieneExtensionPermitida(
            String nombre,
            String... extensiones
    ) {
        String nombreMinusculas = nombre.toLowerCase(Locale.ROOT);
        for (String extension : extensiones) {
            if (nombreMinusculas.endsWith(
                    "." + extension.toLowerCase(Locale.ROOT)
            )) {
                return true;
            }
        }
        return false;
    }

    private static File obtenerDirectorioPersonal() {
        String rutaPersonal = System.getProperty("user.home", ".");
        File directorioPersonal = new File(rutaPersonal);
        return directorioPersonal.isDirectory()
                ? directorioPersonal
                : new File(".").getAbsoluteFile();
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
