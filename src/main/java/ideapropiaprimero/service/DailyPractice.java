package ideapropiaprimero.service;

import java.time.LocalDate;
import java.util.List;

/** Banco inicial de práctica. La respuesta se publica solo después de un intento razonado. */
public final class DailyPractice {
    public record Exercise(String id,String skill,String question,List<String> options) { }
    private record Item(Exercise exercise,int answer,String explanation) { }
    private static Item item(String id,String skill,String question,int correct,String explanation,String... options){return new Item(new Exercise(id,skill,question,List.of(options)),correct,explanation);}
    private static final List<Item> BANK=List.of(
        item("logic-1","Evaluar conclusiones","Todos los gatos del refugio tienen chip. Luna tiene chip. ¿Qué podemos concluir?",2,"Tener chip no demuestra que Luna sea un gato del refugio: otras mascotas también pueden tenerlo.","Luna es un gato del refugio","Luna no es un gato","No basta la información"),
        item("evidence-1","Evaluar evidencia","Un amigo mejoró una nota después de cambiar de lápiz. ¿Qué información ayudaría más a evaluar si el lápiz fue la causa?",1,"Comparar condiciones semejantes y controlar otros cambios aporta más evidencia que un caso aislado.","El color del lápiz","Si también cambió cuánto estudió o la dificultad del examen","Si le gusta la marca"),
        item("invariant-1","Reconocer invariantes","Para mantener una balanza equilibrada, si agregas 3 unidades a un lado, ¿qué debes hacer en el otro?",0,"Agregar la misma cantidad conserva la igualdad entre ambos lados.","Agregar 3","Quitar 3","No cambiar nada"),
        item("counterexample-1","Buscar contraejemplos","Alguien afirma que todos los números primos son impares. ¿Qué dato refuta la afirmación?",1,"El 2 es primo y par. Un contraejemplo basta para refutar una afirmación universal.","El 9 es impar","El 2 es primo","El 11 es primo"),
        item("algorithm-1","Comprobar algoritmos","Un algoritmo para hallar el máximo empieza suponiendo que el máximo es 0. ¿Con qué entrada conviene probarlo?",2,"Si todos los valores son negativos, devolver 0 sería incorrecto porque ni siquiera pertenece al arreglo.","[2, 8, 4]","[0, 1, 2]","[-8, -3, -5]"),
        item("planning-1","Planificar una comprobación","Dos explicaciones distintas predicen lo mismo en tu primer experimento. ¿Qué experimento aporta más información?",0,"Un caso donde las predicciones difieren permite distinguir entre las explicaciones.","Uno donde predigan resultados diferentes","Repetir exactamente el mismo sin medir nada nuevo","Elegir la explicación más larga"),
        item("probability-1","Razonar con incertidumbre","Una moneda equilibrada cayó cara cinco veces. En un lanzamiento independiente, ¿cuál es la probabilidad de cara?",1,"Los lanzamientos independientes no compensan resultados anteriores; la probabilidad sigue siendo 1/2.","Menos de 1/2","1/2","Más de 1/2"),
        item("transfer-1","Comprobar comprensión","Después de leer una solución, ¿qué prueba aporta mejor evidencia de que entendiste el método?",2,"Resolver un caso nuevo y explicar tus decisiones aporta más evidencia que reconocer o copiar pasos.","Copiarla sin errores","Reconocer todos sus símbolos","Resolver otro problema y justificar tus pasos")
    );
    private DailyPractice() { }
    private static Item today(LocalDate date){return BANK.get(Math.floorMod(date.toEpochDay(),BANK.size()));}
    public static Exercise exercise(LocalDate date){return today(date).exercise();}
    public static boolean correct(LocalDate date,int choice){return today(date).answer()==choice;}
    public static String feedback(LocalDate date,int choice){return (correct(date,choice)?"Tu elección es correcta. ":"Revisa esta idea: ")+today(date).explanation()+" Compara esta explicación con tu razonamiento.";}
}
