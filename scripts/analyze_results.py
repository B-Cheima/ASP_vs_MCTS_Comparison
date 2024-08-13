import os
import matplotlib.pyplot as plt

# Chemins des répertoires et des fichiers
RESULTS_DIR = "C:/Users/Lenovo/ASP_vs_MCTS_Comparison/results"
BENCHMARKS = ["blocksworld", "depot", "gripper", "logistics"]
PLANNERS = ["ASP", "MCTS"]

def read_output_metrics(file_path):
    execution_time = None
    makespan = None
    try:
        with open(file_path, 'r') as file:
            lines = file.readlines()
            for line in lines:
                if "Execution Time:" in line:
                    try:
                        time_str = line.split(":")[1].strip().replace("ms", "").strip()
                        execution_time = float(time_str)
                    except ValueError:
                        print(f"Erreur de conversion du temps d'exécution dans {file_path}: {line}")
                if "Makespan:" in line:
                    try:
                        makespan_str = line.split(":")[1].strip()
                        makespan = int(makespan_str)
                    except ValueError:
                        print(f"Erreur de conversion du makespan dans {file_path}: {line}")
            return execution_time, makespan
    except FileNotFoundError:
        print(f"Fichier non trouvé : {file_path}")
        return None, None

# Stocker les résultats pour chaque problème
results = {planner: {benchmark: [] for benchmark in BENCHMARKS} for planner in PLANNERS}

# Lecture des résultats pour chaque benchmark et chaque planificateur
for benchmark in BENCHMARKS:
    for planner in PLANNERS:
        for i in range(1, 11):  # Supposons que vous avez 10 problèmes par benchmark
            output_file = os.path.join(RESULTS_DIR, f"{benchmark}_p{i:02d}_{planner}_output.txt")

            execution_time, makespan = read_output_metrics(output_file)
            
            if execution_time is not None and makespan is not None:
                results[planner][benchmark].append((execution_time, makespan))

# Classer les problèmes selon la difficulté pour ASP (HSP) sur la base du makespan
sorted_problems = {}
for benchmark in BENCHMARKS:
    if len(results["ASP"][benchmark]) == 10:
        sorted_problems[benchmark] = sorted(range(10), key=lambda i: results["ASP"][benchmark][i][1])
    else:
        print(f"Résultats manquants pour {benchmark}, le classement des problèmes est ignoré.")

# Noms des problèmes de p1 à p10
problem_names = [f"p{i+1}" for i in range(10)]

# Fonction pour tracer les résultats
def plot_results(metric, ylabel):
    for benchmark in BENCHMARKS:
        if benchmark in sorted_problems:
            plt.figure()
            x_values = problem_names  # Garder les noms des problèmes fixes
            for planner in PLANNERS:
                if metric == "Execution Time":
                    data = [results[planner][benchmark][i][0] for i in sorted_problems[benchmark]]
                elif metric == "Makespan":
                    data = [results[planner][benchmark][i][1] for i in sorted_problems[benchmark]]

                plt.plot(x_values, data, label=planner)
            
            plt.xlabel("Problèmes classés par difficulté (p1 à p10)")
            plt.ylabel(ylabel)
            plt.title(f"{ylabel} pour {benchmark}")
            plt.legend()
            plt.savefig(os.path.join(RESULTS_DIR, f"{benchmark}_{metric}.png"))
            plt.close()

# Tracer les résultats pour le temps d'exécution et le makespan
plot_results("Execution Time", "Temps d'exécution (ms)")
plot_results("Makespan", "Makespan")
