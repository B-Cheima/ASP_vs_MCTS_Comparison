#!/bin/bash

# Chemins des répertoires
VAL_PATH="C:/Users/Lenovo/VAL/build/bin/Validate.exe"
RESULTS_DIR="C:/Users/Lenovo/ASP_vs_MCTS_Comparison/results"
RESOURCES_DIR="C:/Users/Lenovo/ASP_vs_MCTS_Comparison/resources"
LOG_FILE="C:/Users/Lenovo/ASP_vs_MCTS_Comparison/results/validation_results.txt"

# Supprimer l'ancien fichier de log s'il existe
if [ -f "$LOG_FILE" ]; then
    rm "$LOG_FILE"
fi

# Définir les benchmarks et le nombre de problèmes correspondants
declare -A benchmarks
benchmarks=( ["blocksworld"]=10 ["depot"]=10 ["gripper"]=6 ["logistics"]=10 )

# Boucle sur chaque benchmark
for benchmark in "${!benchmarks[@]}"; do
    num_problems=${benchmarks[$benchmark]}
    domain_file="$RESOURCES_DIR/$benchmark/domain.pddl"
    
    # Boucle sur chaque problème
    for ((i=1; i<=num_problems; i++)); do
        problem_file="$RESOURCES_DIR/$benchmark/p$(printf "%02d" $i).pddl"
        
        # Test pour ASP
        asp_plan="$RESULTS_DIR/${benchmark}_p$(printf "%02d" $i)_ASP_output.txt"
        echo "-----------" >> "$LOG_FILE"
        echo "Benchmark: $benchmark, Problem: p$(printf "%02d" $i), Planner: ASP" >> "$LOG_FILE"
        "$VAL_PATH" "$domain_file" "$problem_file" "$asp_plan" >> "$LOG_FILE" 2>&1
        
        # Test pour MCTS
        mcts_plan="$RESULTS_DIR/${benchmark}_p$(printf "%02d" $i)_MCTS_output.txt"
        echo "-----------" >> "$LOG_FILE"
        echo "Benchmark: $benchmark, Problem: p$(printf "%02d" $i), Planner: MCTS" >> "$LOG_FILE"
        "$VAL_PATH" "$domain_file" "$problem_file" "$mcts_plan" >> "$LOG_FILE" 2>&1
        
        echo "Validation terminée pour $benchmark problème p$(printf "%02d" $i)"
    done
done

echo "Résultats enregistrés dans $LOG_FILE"
