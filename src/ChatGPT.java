

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class ChatGPT {

    // Dirección de la API
    private static final String API_URL =
            "https://api.openai.com/v1/responses";

    // Modelo que queremos utilizar
    private static final String MODELO = "gpt-5.6-luna";

    // La clave se carga desde pai.dt al iniciar la aplicacion y no queda
    // incrustada en el codigo ni dentro del JAR.
    // ---------------------------------------------------------
    // PREGUNTAR A CHATGPT
    // ---------------------------------------------------------

    public static String preguntar(String pregunta) {

        String apiKey = new Datos().getChatGptAPI();
        if (apiKey == null || apiKey.isBlank()) {
            //return "ERROR: No existe la variable OPENAI_API_KEY";
            return "ERROR: No _API_KEY";
        }

        try {

            HttpClient cliente = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String preguntaJSON = escaparJSON(pregunta);

            String json = """
                    {
                        "model": "%s",
                        "input": "%s"
                    }
                    """.formatted(MODELO, preguntaJSON);


            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    json,
                                    StandardCharsets.UTF_8
                            )
                    )
                    .build();


            HttpResponse<String> response =
                    cliente.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );


            // Comprobamos que la petición ha funcionado
            if (response.statusCode() != 200) {

                return "ERROR HTTP "
                        + response.statusCode()
                        + "\n\n"
                        + response.body();
            }


            // Extraemos solamente el texto de la respuesta
            return extraerRespuesta(response.body());


        } catch (HttpTimeoutException e) {

            return mensajeSeguro(
                    "5060",
                    "ERROR: La corrección ha superado el tiempo máximo de espera."
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            return mensajeSeguro("5061", "Corrección cancelada.");

        } catch (Exception e) {

            e.printStackTrace();

            //return "ERROR al conectar con IA:\n"+ e.getMessage();
            return mensajeSeguro("4220", "Sin acceso a IA !!")
                    + "\n"
                    + mensajeSeguro(
                            "4230",
                            "Comprobar la conexión a internet y los permisos API"
                    );
        }
    }

    private static String mensajeSeguro(
            String codigo,
            String mensajePredeterminado
    ) {
        try {
            return new MetodosLb().leerMensajeIdioma(codigo);
        } catch (IOException | SecurityException e) {
            return mensajePredeterminado;
        }
    }


    // ---------------------------------------------------------
    // ESCAPAR TEXTO PARA JSON
    // ---------------------------------------------------------

    private static String escaparJSON(String texto) {

        if (texto == null) {
            return "";
        }

        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }


    // ---------------------------------------------------------
    // EXTRAER EL TEXTO DEL JSON
    // ---------------------------------------------------------

    private static String extraerRespuesta(String json) {

        int posicionOutput =
                json.indexOf("\"type\":\"output_text\"");

        if (posicionOutput == -1) {
            posicionOutput =
                    json.indexOf("\"type\": \"output_text\"");
        }

        if (posicionOutput == -1) {
            return "No se encontró output_text.\n\n" + json;
        }

        int posicionText =
                json.indexOf("\"text\"", posicionOutput);

        if (posicionText == -1) {
            return "No se encontró text.\n\n" + json;
        }

        int dosPuntos =
                json.indexOf(":", posicionText);

        int primeraComilla =
                json.indexOf("\"", dosPuntos);

        if (primeraComilla == -1) {
            return json;
        }

        StringBuilder resultado = new StringBuilder();

        for (int i = primeraComilla + 1; i < json.length(); i++) {

            char c = json.charAt(i);

            if (c == '"') {
                break;
            }

            if (c == '\\' && i + 1 < json.length()) {

                char siguiente = json.charAt(++i);

                switch (siguiente) {

                    case 'n':
                        resultado.append('\n');
                        break;

                    case 'r':
                        resultado.append('\r');
                        break;

                    case 't':
                        resultado.append('\t');
                        break;

                    case '"':
                        resultado.append('"');
                        break;

                    case '\\':
                        resultado.append('\\');
                        break;

                    case '/':
                        resultado.append('/');
                        break;

                    case 'b':
                        resultado.append('\b');
                        break;

                    case 'f':
                        resultado.append('\f');
                        break;

                    case 'u':

                        // Convierte uXXXX a carácter Unicode

                        if (i + 4 < json.length()) {

                            String hexadecimal =
                                    json.substring(i + 1, i + 5);

                            try {

                                int codigo =
                                        Integer.parseInt(
                                                hexadecimal, 16);

                                resultado.append((char) codigo);

                                i += 4;

                            } catch (NumberFormatException e) {

                                resultado.append("\\u")
                                        .append(hexadecimal);

                                i += 4;
                            }
                        }

                        break;

                    default:
                        resultado.append(siguiente);
                }

            } else {

                resultado.append(c);
            }
        }

        return resultado.toString();
    }

    // ---------------------------------------------------------
    // PRUEBA
    // ---------------------------------------------------------

    /*
    public static void main(String[] args) {

        System.out.println("Preguntando a ChatGPT...\n");

        String respuesta =
                preguntar(
                        "Actúa como profesor.\n" +
                        "Evalúa de 0 a 10 la respuesta.\n" +
                        "Explica brevemente los errores encontrados.\n" +
                        "La explicación no debe superar las 200 palabras.\n" +
                        "PREGUNTA : Explica la teoría de Darwin\n" +
                        "RESPUESTA: La teoría de Darwin intenta explicar el origen de ..."
                      );

        System.out.println(respuesta);
    }

     */

}
