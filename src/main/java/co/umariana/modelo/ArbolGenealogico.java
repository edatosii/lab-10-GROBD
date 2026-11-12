package co.umariana.modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * El árbol genealógico: un SUJETO (posición "1") con sus ASCENDIENTES (padres, abuelos... en Ahnentafel)
 * y sus DESCENDIENTES (hijos, nietos... en d'Aboville). Ver la clase Posicion.
 *
 * Son en realidad dos árboles unidos en la raíz:
 *
 *     ascendientes:  árbol BINARIO. Cada persona tiene como mucho un padre (2n) y una madre (2n + 1).
 *     descendientes: árbol GENERAL. Cada persona tiene una lista de hijos (1.1, 1.2, ...) ordenada por número.
 *
 * El árbol solo guarda a los miembros que EXISTEN: no hay "huecos" ni personas vacías. Puede faltar el padre
 * de alguien, o el hijo 1.1 aunque exista el 1.2.
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 2)
 * ------------------------------------------
 * 1. Implemente el árbol con sus PROPIOS NODOS: una clase interna "Nodo" con la Persona, el nodo del
 *    padre, el nodo de la madre y la lista de hijos. NO se permite usar Map ni Set (las pruebas lo
 *    revisan): para buscar una posición recorra el árbol siguiendo su camino
 *      - Ahnentafel: los bits de n después del primer 1 dicen el camino (0 = padre, 1 = madre).
 *        Ej.: 5 = 101 -> desde el sujeto: padre (0), luego madre (1).
 *      - D'Aboville: cada número de la posición dice a qué hijo bajar. Ej.: 1.2.3 -> hijo 2, luego su hijo 3.
 *    Se permite usar List/ArrayList para la lista de hijos.
 * 2. Todos los errores de las reglas se informan con {@link ArbolInvalidoException} (un mensaje claro).
 *    Los datos inválidos de una Persona los detecta Persona.validar() (IllegalArgumentException).
 * 3. NO modifique las pruebas (src/test).
 *
 * REGLAS DEL ÁRBOL
 *   - La posición "1" (el sujeto) existe siempre y no se puede eliminar.
 *   - Una posición no puede repetirse.
 *   - Un ascendiente n solo se puede agregar si ya existe su hijo n / 2, y debe haber NACIDO ANTES
 *     (año de nacimiento menor) que ese hijo.
 *   - Un descendiente solo se puede agregar si ya existe su progenitor, y debe haber NACIDO DESPUÉS
 *     (año de nacimiento mayor) que su progenitor.
 */
public class ArbolGenealogico {

    private static final class Nodo {
        private final Persona persona;
        private Nodo padre;
        private Nodo madre;
        private Nodo progenitor;
        private final List<Nodo> hijos = new ArrayList<>();

        private Nodo(Persona persona) {
            this.persona = persona;
        }
    }

    private final Nodo raiz;
    private int cantidad;

    /**
     * Crea un árbol que solo tiene al sujeto.
     *
     * @throws IllegalArgumentException si la persona no es válida (Persona.validar)
     * @throws ArbolInvalidoException si su posición no es "1"
     */
    public ArbolGenealogico(Persona sujeto) {
        if (sujeto == null) {
            throw new IllegalArgumentException("El sujeto no puede ser null.");
        }
        sujeto.validar();
        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException("El sujeto debe estar en la posición 1.");
        }
        raiz = new Nodo(sujeto);
        cantidad = 1;
    }

    /** @return el sujeto (la persona de la posición "1") */
    public Persona sujeto() {
        return raiz.persona;
    }

    /** @return cuántos miembros tiene el árbol (el sujeto cuenta; un árbol nuevo tiene 1) */
    public int tamano() {
        return cantidad;
    }

    /**
     * @return la persona de esa posición, o null si no existe en el árbol o si la posición no es válida
     */
    public Persona buscar(String posicion) {
        if (!Posicion.esValida(posicion)) {
            return null;
        }
        Nodo nodo = buscarNodo(raiz, posicion);
        return nodo == null ? null : nodo.persona;
    }

    /** @return true si hay alguien en esa posición (false también si la posición no es válida) */
    public boolean contiene(String posicion) {
        return buscar(posicion) != null;
    }

    private Nodo buscarNodo(Nodo nodo, String posicion) {
        if (nodo == null) {
            return null;
        }
        if (nodo.persona.getPosicion().equals(posicion)) {
            return nodo;
        }

        Nodo encontrado = buscarNodo(nodo.padre, posicion);
        if (encontrado != null) {
            return encontrado;
        }
        encontrado = buscarNodo(nodo.madre, posicion);
        if (encontrado != null) {
            return encontrado;
        }
        for (Nodo hijo : nodo.hijos) {
            encontrado = buscarNodo(hijo, posicion);
            if (encontrado != null) {
                return encontrado;
            }
        }
        return null;
    }

    /**
     * Agrega una persona en la posición que ella misma trae (la usa el constructor del árbol a partir del texto).
     * El orden de llamada importa: primero debe existir la persona de la que depende (ver REGLAS DEL ÁRBOL).
     *
     * @return la misma persona
     * @throws IllegalArgumentException si la persona no es válida (Persona.validar)
     * @throws ArbolInvalidoException si la posición ya existe (o es "1"), si falta la persona de la que depende,
     *         o si los años de nacimiento no cumplen las reglas
     */
    public Persona agregar(Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("La persona no puede ser null.");
        }
        persona.validar();

        String posicion = persona.getPosicion();
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("El sujeto ya existe en el árbol.");
        }
        if (contiene(posicion)) {
            throw new ArbolInvalidoException("Ya existe una persona en la posición " + posicion + ".");
        }

        Nodo nuevo = new Nodo(persona);
        if (Posicion.esAscendiente(posicion)) {
            Nodo hijo = buscarNodo(raiz, Posicion.posicionDelHijoEnLinea(posicion));
            if (hijo == null) {
                throw new ArbolInvalidoException("No existe el hijo en línea de la posición " + posicion + ".");
            }
            if (persona.getNacimiento() >= hijo.persona.getNacimiento()) {
                throw new ArbolInvalidoException("El ascendiente debe haber nacido antes que su hijo.");
            }

            if (Integer.parseInt(posicion) % 2 == 0) {
                if (hijo.padre != null) {
                    throw new ArbolInvalidoException("Ya existe el padre de la posición " + hijo.persona.getPosicion() + ".");
                }
                hijo.padre = nuevo;
            } else {
                if (hijo.madre != null) {
                    throw new ArbolInvalidoException("Ya existe la madre de la posición " + hijo.persona.getPosicion() + ".");
                }
                hijo.madre = nuevo;
            }
        } else {
            String posicionProgenitor = Posicion.posicionDelProgenitor(posicion);
            Nodo progenitor = buscarNodo(raiz, posicionProgenitor);
            if (progenitor == null) {
                throw new ArbolInvalidoException("No existe el progenitor de la posición " + posicion + ".");
            }
            if (persona.getNacimiento() <= progenitor.persona.getNacimiento()) {
                throw new ArbolInvalidoException("El descendiente debe haber nacido después de su progenitor.");
            }

            nuevo.progenitor = progenitor;
            int ordenNuevo = Posicion.orden(posicion);
            int indice = 0;
            while (indice < progenitor.hijos.size()
                    && Posicion.orden(progenitor.hijos.get(indice).persona.getPosicion()) < ordenNuevo) {
                indice++;
            }
            progenitor.hijos.add(indice, nuevo);
        }

        cantidad++;
        return persona;
    }

    /**
     * Agrega el PADRE de la persona de esa posición, en la posición 2n (ver Posicion.posicionDelPadre).
     * Solo se pueden agregar padres al sujeto y a sus ascendientes.
     *
     * @return la persona creada
     * @throws ArbolInvalidoException si la posición no existe, si no es el sujeto ni un ascendiente, si ese padre
     *         ya existe o si no cumple las reglas del árbol
     * @throws IllegalArgumentException si los datos de la persona no son válidos
     */
    public Persona agregarPadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if ((!Posicion.esSujeto(posicion) && !Posicion.esAscendiente(posicion))
                || !contiene(posicion)) {
            throw new ArbolInvalidoException("La posición debe ser el sujeto o un ascendiente existente.");
        }
        Persona padre = new Persona(Posicion.posicionDelPadre(posicion), nombre, nacimiento, fallecimiento);
        return agregar(padre);
    }

    /**
     * Igual que agregarPadre, pero agrega la MADRE (posición 2n + 1).
     */
    public Persona agregarMadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if ((!Posicion.esSujeto(posicion) && !Posicion.esAscendiente(posicion))
                || !contiene(posicion)) {
            throw new ArbolInvalidoException("La posición debe ser el sujeto o un ascendiente existente.");
        }
        Persona madre = new Persona(Posicion.posicionDeLaMadre(posicion), nombre, nacimiento, fallecimiento);
        return agregar(madre);
    }

    /**
     * Agrega un HIJO a la persona de esa posición. Solo se pueden agregar hijos al sujeto y a sus descendientes.
     * Su posición es la del progenitor + "." + el número siguiente al MAYOR número de hijo que ya exista
     * (o 1 si todavía no tiene hijos). Ej.: si "1" tiene los hijos 1.1 y 1.3, el nuevo es 1.4.
     *
     * @return la persona creada
     * @throws ArbolInvalidoException si la posición no existe, si no es el sujeto ni un descendiente, o si no cumple
     *         las reglas del árbol
     * @throws IllegalArgumentException si los datos de la persona no son válidos
     */
    public Persona agregarHijo(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if ((!Posicion.esSujeto(posicion) && !Posicion.esDescendiente(posicion))
                || !contiene(posicion)) {
            throw new ArbolInvalidoException("La posición debe ser el sujeto o un descendiente existente.");
        }

        Nodo progenitor = buscarNodo(raiz, posicion);
        int mayorOrden = 0;
        for (Nodo hijo : progenitor.hijos) {
            mayorOrden = Math.max(mayorOrden, Posicion.orden(hijo.persona.getPosicion()));
        }
        if (mayorOrden >= Posicion.MAX_ORDEN) {
            throw new ArbolInvalidoException("Se alcanzó el máximo de hijos permitidos.");
        }
        String posicionHijo = posicion + "." + (mayorOrden + 1);
        return agregar(new Persona(posicionHijo, nombre, nacimiento, fallecimiento));
    }

    /**
     * Elimina a la persona de esa posición y TODO lo que depende de ella:
     *   - si es un ascendiente: ella y todos sus propios ascendientes (sus padres, abuelos...);
     *   - si es un descendiente: ella y todos sus propios descendientes (hijos, nietos...).
     * Las demás posiciones NO cambian de número.
     *
     * @return cuántas personas se eliminaron (ella incluida)
     * @throws ArbolInvalidoException si la posición es "1" (el sujeto no se puede eliminar) o si no existe
     */
    public int eliminar(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("No se puede eliminar al sujeto.");
        }
        if (!Posicion.esValida(posicion)) {
            throw new ArbolInvalidoException("La posición no es válida.");
        }

        int eliminados;
        if (Posicion.esAscendiente(posicion)) {
            Nodo nodo = buscarNodo(raiz, posicion);
            if (nodo == null) {
                throw new ArbolInvalidoException("No existe la posición " + posicion + ".");
            }
            eliminados = contarRamaAscendiente(nodo);
            if (!desconectarAscendiente(raiz, posicion)) {
                throw new ArbolInvalidoException("No se pudo eliminar la posición " + posicion + ".");
            }
        } else {
            Nodo nodo = buscarNodo(raiz, posicion);
            if (nodo == null) {
                throw new ArbolInvalidoException("No existe la posición " + posicion + ".");
            }
            eliminados = contarDescendientes(nodo);
            Nodo progenitor = nodo.progenitor;
            progenitor.hijos.remove(nodo);
        }

        cantidad -= eliminados;
        return eliminados;
    }

    private int contarRamaAscendiente(Nodo nodo) {
        if (nodo == null) {
            return 0;
        }
        return 1 + contarRamaAscendiente(nodo.padre) + contarRamaAscendiente(nodo.madre);
    }

    private int contarDescendientes(Nodo nodo) {
        int total = 1;
        for (Nodo hijo : nodo.hijos) {
            total += contarDescendientes(hijo);
        }
        return total;
    }

    private boolean desconectarAscendiente(Nodo nodo, String posicion) {
        if (nodo == null) {
            return false;
        }
        if (nodo.padre != null && nodo.padre.persona.getPosicion().equals(posicion)) {
            nodo.padre = null;
            return true;
        }
        if (desconectarAscendiente(nodo.padre, posicion)) {
            return true;
        }
        if (nodo.madre != null && nodo.madre.persona.getPosicion().equals(posicion)) {
            nodo.madre = null;
            return true;
        }
        if (desconectarAscendiente(nodo.madre, posicion)) {
            return true;
        }
        for (Nodo hijo : nodo.hijos) {
            if (desconectarAscendiente(hijo, posicion)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return todos los miembros en este orden: el sujeto; luego los ascendientes por número Ahnentafel
     *         ascendente (2, 3, 4, 5...); luego los descendientes en PREORDEN (cada persona y después sus hijos:
     *         1.1, 1.1.1, 1.1.2, 1.2, ...). La lista es una copia nueva: modificarla no cambia el árbol.
     *         Este orden garantiza que cada persona aparece DESPUÉS de la que necesita (útil para guardar y cargar).
     */
    public List<Persona> miembros() {
        List<Persona> personas = new ArrayList<>();
        personas.add(raiz.persona);

        List<Nodo> ascendientes = new ArrayList<>();
        recopilarAscendientes(raiz.padre, ascendientes);
        recopilarAscendientes(raiz.madre, ascendientes);
        ascendientes.sort(Comparator.comparingInt(n -> Integer.parseInt(n.persona.getPosicion())));
        for (Nodo nodo : ascendientes) {
            personas.add(nodo.persona);
        }

        recopilarDescendientes(raiz, personas);
        return personas;
    }

    /** @return solo los ascendientes, por número Ahnentafel ascendente (sin el sujeto) */
    public List<Persona> ascendientes() {
        List<Nodo> nodos = new ArrayList<>();
        recopilarAscendientes(raiz.padre, nodos);
        recopilarAscendientes(raiz.madre, nodos);
        nodos.sort(Comparator.comparingInt(n -> Integer.parseInt(n.persona.getPosicion())));

        List<Persona> personas = new ArrayList<>();
        for (Nodo nodo : nodos) {
            personas.add(nodo.persona);
        }
        return personas;
    }

    /** @return solo los descendientes, en preorden (sin el sujeto) */
    public List<Persona> descendientes() {
        List<Persona> personas = new ArrayList<>();
        recopilarDescendientes(raiz, personas);
        return personas;
    }

    private void recopilarAscendientes(Nodo nodo, List<Nodo> resultado) {
        if (nodo == null) {
            return;
        }
        resultado.add(nodo);
        recopilarAscendientes(nodo.padre, resultado);
        recopilarAscendientes(nodo.madre, resultado);
    }

    private void recopilarDescendientes(Nodo nodo, List<Persona> resultado) {
        for (Nodo hijo : nodo.hijos) {
            resultado.add(hijo.persona);
            recopilarDescendientes(hijo, resultado);
        }
    }
}
