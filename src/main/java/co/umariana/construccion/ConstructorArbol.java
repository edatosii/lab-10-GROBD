package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construye un árbol genealógico a partir de un texto: cada línea es una persona.
 *
 *     posicion;nombre;nacimiento;fallecimiento
 *
 * Ejemplo (el fallecimiento puede quedar vacío):
 *
 *     1;John Doe;2002;
 *     2;Henry Doe;1920;1990
 *     3;Maria Doe;1921;
 *     1.1;Timmy Doe;2021;
 *     1.2;Amy Doe;2026;
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 3)
 * ------------------------------------------
 * Reemplace el "throw new UnsupportedOperationException" por su implementación. Esta clase usa lo que ya
 * hizo en las etapas anteriores (Posicion, Persona y ArbolGenealogico). NO modifique las pruebas (src/test).
 *
 * REGLAS
 *   1. Si el texto es null o no tiene ninguna línea con contenido: ArbolInvalidoException (mensaje: "El texto está vacío...").
 *   2. Las líneas pueden terminar en \n o en \r\n. Las líneas en blanco se ignoran, pero los números de línea
 *      que se informan cuentan TODAS las líneas del texto (la primera es la línea 1).
 *   3. Cada línea tiene 4 campos separados por ';' (el cuarto, el fallecimiento, puede estar vacío). Se acepta
 *      también la línea con solo 3 campos y sin el último ';'. Los campos se recortan (trim).
 *      Si hay menos de 3 o más de 4 campos: error.
 *   4. El nacimiento y el fallecimiento deben ser números enteros (el fallecimiento vacío es "no tiene").
 *   5. Cada persona debe ser válida (Persona.validar) y no puede haber dos líneas con la misma posición.
 *   6. Debe existir la persona "1" (el sujeto), pero puede estar en cualquier línea.
 *   7. Las líneas pueden venir en CUALQUIER orden. El constructor debe agregarlas al árbol en un orden en el
 *      que cada persona ya tenga a la que necesita (por ejemplo, primero los ascendientes por número Ahnentafel
 *      ascendente y los descendientes por generación). Las reglas del árbol (ver ArbolGenealogico) las valida el árbol.
 *   8. Todos los errores asociados a una línea se informan con ArbolInvalidoException y un mensaje que EMPIEZA con
 *      "Línea N: " (N es el número de línea de la persona con el problema), por ejemplo:
 *          "Línea 3: la posición 2 está repetida."
 *      Los errores que no son de una línea (texto vacío, falta el sujeto) no llevan ese prefijo.
 *   9. Solo se muestran los miembros que están en el texto: no se crean personas vacías para los que faltan.
 */
public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    /**
     * Convierte el texto en un árbol (ver las reglas arriba).
     *
     * @throws ArbolInvalidoException si el texto no cumple las reglas
     */
    public static ArbolGenealogico construir(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ArbolInvalidoException("El texto está vacío.");
        }

        List<Entrada> entradas = new ArrayList<>();
        Map<String, Integer> lineasPorPosicion = new HashMap<>();
        String[] lineas = texto.split("\\r?\\n", -1);
        Entrada sujeto = null;

        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i].trim();
            if (linea.isEmpty()) {
                continue;
            }

            int numeroLinea = i + 1;
            String[] campos = lineas[i].split(";", -1);
            if (campos.length < 3 || campos.length > 4) {
                throw errorDeLinea(numeroLinea, "se esperaban 3 o 4 campos.");
            }

            String posicion = campos[0].trim();
            String nombre = campos[1].trim();
            int nacimiento;
            Integer fallecimiento = null;
            try {
                nacimiento = Integer.parseInt(campos[2].trim());
                if (campos.length == 4 && !campos[3].trim().isEmpty()) {
                    fallecimiento = Integer.valueOf(campos[3].trim());
                }
            } catch (NumberFormatException e) {
                throw errorDeLinea(numeroLinea, "el año de nacimiento o fallecimiento no es un número entero.");
            }

            Persona persona = new Persona(posicion, nombre, nacimiento, fallecimiento);
            try {
                persona.validar();
            } catch (IllegalArgumentException e) {
                throw errorDeLinea(numeroLinea, e.getMessage());
            }

            if (lineasPorPosicion.putIfAbsent(posicion, numeroLinea) != null) {
                throw errorDeLinea(numeroLinea, "la posición " + posicion + " está repetida.");
            }

            Entrada entrada = new Entrada(persona, numeroLinea);
            entradas.add(entrada);
            if ("1".equals(posicion)) {
                sujeto = entrada;
            }
        }

        if (sujeto == null) {
            throw new ArbolInvalidoException("Falta la posición 1 del sujeto.");
        }

        entradas.remove(sujeto);
        entradas.sort((primera, segunda) -> compararPosiciones(
                primera.persona().getPosicion(), segunda.persona().getPosicion()));

        ArbolGenealogico arbol;
        try {
            arbol = new ArbolGenealogico(sujeto.persona());
        } catch (IllegalArgumentException e) {
            throw errorDeLinea(sujeto.numeroLinea(), e.getMessage());
        }
        for (Entrada entrada : entradas) {
            try {
                arbol.agregar(entrada.persona());
            } catch (IllegalArgumentException e) {
                throw errorDeLinea(entrada.numeroLinea(), e.getMessage());
            }
        }
        return arbol;
    }

    private static ArbolInvalidoException errorDeLinea(int numeroLinea, String mensaje) {
        return new ArbolInvalidoException("Línea " + numeroLinea + ": " + mensaje);
    }

    private static int compararPosiciones(String primera, String segunda) {
        boolean primeraEsDescendiente = primera.indexOf('.') >= 0;
        boolean segundaEsDescendiente = segunda.indexOf('.') >= 0;
        if (primeraEsDescendiente != segundaEsDescendiente) {
            return primeraEsDescendiente ? 1 : -1;
        }

        if (!primeraEsDescendiente) {
            return Integer.compare(Integer.parseInt(primera), Integer.parseInt(segunda));
        }

        String[] partesPrimera = primera.split("\\.");
        String[] partesSegunda = segunda.split("\\.");
        int comparacionGeneracion = Integer.compare(partesPrimera.length, partesSegunda.length);
        if (comparacionGeneracion != 0) {
            return comparacionGeneracion;
        }

        for (int i = 1; i < partesPrimera.length; i++) {
            int comparacionParte = Integer.compare(
                    Integer.parseInt(partesPrimera[i]), Integer.parseInt(partesSegunda[i]));
            if (comparacionParte != 0) {
                return comparacionParte;
            }
        }
        return 0;
    }

    private record Entrada(Persona persona, int numeroLinea) {
    }
}
