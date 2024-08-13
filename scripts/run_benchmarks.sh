#!/bin/bash

# Répertoires
SRC_DIR="C:/Users/Lenovo/ASP_vs_MCTS_Comparison/src"
BIN_DIR="C:/Users/Lenovo/ASP_vs_MCTS_Comparison/bin"
LIB_DIR="C:/Users/Lenovo/ASP_vs_MCTS_Comparison/lib"
PDDL_DIR="C:/Users/Lenovo/ASP_vs_MCTS_Comparison/resources"
RESULTS_DIR="C:/Users/Lenovo/ASP_vs_MCTS_Comparison/results"
mkdir -p $RESULTS_DIR

# Chemins des bibliothèques
CLASSPATH="$LIB_DIR/pddl4j-4.0.0.jar;$LIB_DIR/log4j-api-2.13.3.jar;$LIB_DIR/log4j-core-2.13.3.jar"

# Compilation des fichiers Java
echo "Compilation des fichiers Java..."
javac -cp "$CLASSPATH" -d "$BIN_DIR" "$SRC_DIR/fr/uga/pddl4j/examples/asp/ASP.java" "$SRC_DIR/fr/uga/pddl4j/examples/asp/Node.java"
javac -cp "$CLASSPATH" -d "$BIN_DIR" "$SRC_DIR/fr/uga/pddl4j/examples/mcts/MCTSPlanner.java" "$SRC_DIR/fr/uga/pddl4j/examples/mcts/MCTSNode.java"

if [[ $? -ne 0 ]]; then
    echo "Erreur lors de la compilation. Veuillez vérifier le code source."
    exit 1
fi
echo "Compilation réussie."

# Benchmarks et planificateurs
BENCHMARKS=("blocksworld" "depot" "gripper" "logistics")
PLANNERS=("ASP" "MCTSPlanner")

# Fonction pour exécuter et mesurer les performances
run_benchmark() {
    local planner=$1
    local domain=$2
    local problem=$3
    local result_file=$4

    echo "Exécution de $planner pour $problem"
    { time java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 --illegal-access=deny -Djdk.attach.allowAttachSelf=false -Djol.tryWithSudo=false -cp "$BIN_DIR;$CLASSPATH" fr.uga.pddl4j.examples.${planner} "${domain}" "${problem}" ; } > "${result_file}_output.txt" 2>&1

    echo "Contenu de ${result_file}_output.txt:"
    cat "${result_file}_output.txt"

    # Capturer la ligne contenant le makespan
    grep -i "makespan" "${result_file}_output.txt" > "${result_file}_makespan.txt"

    if [[ ! -s "${result_file}_makespan.txt" ]]; then
        echo "Aucun makespan trouvé pour ${problem} avec ${planner}"
    else
        echo "Makespan trouvé :"
        cat "${result_file}_makespan.txt"
    fi
}

# Exécution des benchmarks pour chaque planificateur
echo "Début de l'exécution des benchmarks"
for benchmark in "${BENCHMARKS[@]}"; do
    DOMAIN_FILE="${PDDL_DIR}/${benchmark}/domain.pddl"
    for problem in ${PDDL_DIR}/${benchmark}/p*.pddl; do
        PROBLEM_FILE="${problem}"
        PROBLEM_NAME=$(basename "${problem}" .pddl)

        # Exécution du planificateur ASP
        run_benchmark "asp.ASP" "${DOMAIN_FILE}" "${PROBLEM_FILE}" "${RESULTS_DIR}/${benchmark}_${PROBLEM_NAME}_ASP"

        # Exécution du planificateur MCTSPlanner
        run_benchmark "mcts.MCTSPlanner" "${DOMAIN_FILE}" "${PROBLEM_FILE}" "${RESULTS_DIR}/${benchmark}_${PROBLEM_NAME}_MCTS"
    done
done
echo "Fin de l'exécution des benchmarks"
