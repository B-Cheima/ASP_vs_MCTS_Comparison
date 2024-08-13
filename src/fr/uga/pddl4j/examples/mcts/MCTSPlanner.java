package fr.uga.pddl4j.examples.mcts;

import java.util.*;

public class MCTSPlanner {

    // Classe représentant la solution générée par l'algorithme MCTS
    public static class MCTSTreeSolution {
        public Map<Long, Map<String, Double>> Q; // Q-values pour chaque état et action
        public Map<Long, Integer> s_visits; // Nombre de visites pour chaque état
        public Map<Long, Map<String, Integer>> a_visits; // Nombre de visites pour chaque action par état

        // Constructeur initialisant les structures de données
        public MCTSTreeSolution() {
            Q = new HashMap<>();
            s_visits = new HashMap<>();
            a_visits = new HashMap<>();
        }

        // Méthode pour copier la solution actuelle
        public MCTSTreeSolution copy() {
            MCTSTreeSolution copy = new MCTSTreeSolution();
            copy.Q = new HashMap<>(this.Q);
            copy.s_visits = new HashMap<>(this.s_visits);
            copy.a_visits = new HashMap<>(this.a_visits);
            return copy;
        }

        // Retourne la meilleure action pour un état donné
        public String getAction(State state) {
            return bestAction(state);
        }

        // Détermine l'action avec la meilleure valeur Q pour un état donné
        public String bestAction(State state) {
            return Collections.max(Q.get(state.hashCode()).entrySet(), Map.Entry.comparingByValue()).getKey();
        }

        // Vérifie si la solution a des valeurs pour l'état donné
        public boolean hasValues() {
            return true;
        }

        // Retourne la meilleure valeur Q pour un état donné
        public double getValue(State state) {
            return Collections.max(Q.get(state.hashCode()).values());
        }

        // Retourne la valeur Q pour une action spécifique dans un état donné
        public double getValue(State state, String action) {
            return Q.get(state.hashCode()).get(action);
        }

        // Vérifie si les valeurs Q pour un état donné sont déjà calculées
        public boolean hasCachedValue(State state) {
            return Q.containsKey(state.hashCode());
        }

        // Vérifie si un état a déjà été visité
        public boolean hasStateNode(State state) {
            return s_visits.containsKey(state.hashCode());
        }
    }

    // Classe abstraite pour estimer la valeur d'une feuille dans l'arbre MCTS
    public static abstract class MCTSLeafEstimator {
        public abstract double estimate(Domain domain, State state, Specification spec, int depth);
    }

    // Estimateur constant pour la valeur des feuilles
    public static class ConstantEstimator extends MCTSLeafEstimator {
        public double value;

        public ConstantEstimator(double value) {
            this.value = value;
        }

        @Override
        public double estimate(Domain domain, State state, Specification spec, int depth) {
            return value;
        }
    }

    // Estimateur aléatoire pour la valeur des feuilles
    public static class RandomRolloutEstimator extends MCTSLeafEstimator {
        @Override
        public double estimate(Domain domain, State state, Specification spec, int depth) {
            return Math.random();
        }
    }

    // Classe principale implémentant l'algorithme MCTS
    public static class MonteCarloTreeSearch {
        public int n_rollouts; // Nombre de simulations
        public int max_depth; // Profondeur maximale de l'arbre
        public Heuristic heuristic; // Heuristique utilisée pour évaluer les états
        public MCTSLeafEstimator estimator; // Estimateur pour les feuilles de l'arbre
        public Random rand; // Générateur de nombres aléatoires

        // Constructeur de l'algorithme MCTS avec initialisation des paramètres
        public MonteCarloTreeSearch(int n_rollouts, int max_depth, Heuristic heuristic, MCTSLeafEstimator estimator) {
            this.n_rollouts = 500; // Réduction du nombre de simulations pour se concentrer sur les chemins prometteurs
            this.max_depth = 50; // Limitation de la profondeur pour éviter des explorations trop profondes
            this.heuristic = heuristic;
            this.estimator = estimator;
            this.rand = new Random(12345); // Seed fixe pour reproductibilité
        }

        // Méthode principale pour résoudre un problème avec l'algorithme MCTS
        public MCTSTreeSolution solve(Domain domain, State state, Specification spec) {
            long startTime = System.currentTimeMillis(); // Commence à mesurer le temps d'exécution

            MCTSTreeSolution sol = new MCTSTreeSolution(); // Crée une nouvelle solution
            insertNode(sol, domain, state, spec); // Insère le nœud initial dans l'arbre
            int totalSteps = 0; // Compteur du nombre total d'étapes
            Set<Long> visitedStates = new HashSet<>(); // Ensemble des états visités

            // Boucle principale des simulations
            for (int n = 0; n < n_rollouts; n++) {
                State currentState = state;
                List<State> sVisited = new ArrayList<>();
                List<String> aVisited = new ArrayList<>();
                boolean goalReached = false;
                for (int t = 0; t < max_depth; t++) {
                    if (isGoal(spec, domain, currentState, PDDL.no_op)) { // Vérifie si l'objectif est atteint
                        System.out.println("Goal reached at depth " + t);
                        goalReached = true;
                        break;
                    }
                    List<String> actions = available(domain, currentState); // Actions disponibles pour l'état courant
                    if (actions.isEmpty()) {
                        System.out.println("No available actions at depth " + t);
                        break;
                    }
                    String action = selectRandomAction(actions); // Sélectionne une action aléatoire
                    sVisited.add(currentState); // Ajoute l'état courant à la liste des états visités
                    aVisited.add(action); // Ajoute l'action à la liste des actions visitées
                    System.out.println("Taking action: " + action);
                    State nextState = transition(domain, currentState, action); // Transition vers le nouvel état
                    if (nextState.equals(currentState)) {
                        System.out.println("State did not change with action: " + action);
                    } else {
                        System.out.println("State changed from " + currentState.hashCode() + " to " + nextState.hashCode());
                        currentState = nextState; // Met à jour l'état courant
                    }
                    totalSteps++;
                    if (!sol.hasStateNode(currentState) && !visitedStates.contains((long) currentState.hashCode())) {
                        visitedStates.add((long) currentState.hashCode());
                        insertNode(sol, domain, currentState, spec); // Insère le nouvel état dans l'arbre
                        double value = Math.pow(getDiscount(spec), t) * estimator.estimate(domain, currentState, spec, max_depth - t); // Estime la valeur de la feuille
                        break;
                    }
                }
                if (goalReached) {
                    break;
                }
                if (!sVisited.isEmpty() && !aVisited.isEmpty()) {
                    backpropagate(sol, sVisited, aVisited, spec, domain); // Met à jour les valeurs Q avec la propagation du retour
                }
            }

            long endTime = System.currentTimeMillis(); // Fin du chronométrage
            long executionTime = endTime - startTime; // Calcul du temps d'exécution

            System.out.println("Execution Time: " + executionTime + " ms");
            System.out.println("Makespan: " + totalSteps);

            return sol; // Retourne la solution trouvée
        }

        // Sélectionne une action aléatoire parmi les actions disponibles
        private String selectRandomAction(List<String> actions) {
            return actions.get(rand.nextInt(actions.size())); // Utilise le générateur aléatoire avec seed fixe
        }

        // Insère un nouveau nœud dans l'arbre MCTS pour un état donné
        private void insertNode(MCTSTreeSolution sol, Domain domain, State state, Specification spec) {
            List<String> actions = available(domain, state); // Récupère les actions disponibles
            Map<String, Double> qs = new HashMap<>(); // Initialisation des valeurs Q
            for (String action : actions) {
                State nextState = transition(domain, state, action); // Transition vers le nouvel état
                double reward = getReward(spec, domain, state, action, nextState); // Calcule la récompense pour l'action
                double heuristicValue = heuristic.compute(domain, nextState, spec); // Calcule la valeur heuristique pour le nouvel état
                qs.put(action, getDiscount(spec) * (-heuristicValue) + reward); // Calcule et stocke la valeur Q pour chaque action
            }
            long stateId = state.hashCode();
            sol.Q.put(stateId, qs); // Associe les valeurs Q au nouvel état
            sol.a_visits.put(stateId, new HashMap<>()); // Initialisation des visites d'action
            for (String action : actions) {
                sol.a_visits.get(stateId).put(action, 0); // Initialise les compteurs de visites pour chaque action
            }
            sol.s_visits.put(stateId, 0); // Initialise le compteur de visites de l'état
        }

        // Propage les récompenses en remontant dans l'arbre MCTS
        private void backpropagate(MCTSTreeSolution sol, List<State> sVisited, List<String> aVisited, Specification spec, Domain domain) {
            State nextState = sVisited.get(sVisited.size() - 1); // Dernier état visité
            double value = 0.0;
            for (int i = sVisited.size() - 1; i >= 0; i--) {
                State state = sVisited.get(i);
                String action = aVisited.get(i);
                long stateId = state.hashCode();
                sol.s_visits.put(stateId, sol.s_visits.get(stateId) + 1); // Incrémente le compteur de visites pour l'état
                sol.a_visits.get(stateId).put(action, sol.a_visits.get(stateId).get(action) + 1); // Incrémente le compteur de visites pour l'action
                double reward = getReward(spec, domain, state, action, nextState); // Calcule la récompense pour l'action
                value = getDiscount(spec) * value + reward; // Met à jour la valeur avec le facteur de discount
                sol.Q.get(stateId).put(action, sol.Q.get(stateId).get(action) + (value - sol.Q.get(stateId).get(action)) / sol.a_visits.get(stateId).get(action)); // Mise à jour de la valeur Q
                nextState = state;
            }
        }

        // Vérifie si l'état courant est un état objectif
        private boolean isGoal(Specification spec, Domain domain, State state, String action) {
            // Logique de vérification de l'objectif (à remplacer par la logique réelle)
            System.out.println("Checking goal for state: " + state.hashCode());
            // Implémentation simplifiée : objectif atteint si l'ID de l'état est un multiple de 100
            return state.hashCode() % 100 == 0 && state.hashCode() != domain.hashCode();
        }

        // Transitionne vers un nouvel état en appliquant une action
        private State transition(Domain domain, State state, String action) {
            // Logique de transition d'état (à remplacer par la logique réelle)
            System.out.println("Transitioning state with action: " + action);
            // Implémentation simplifiée : simule un changement d'état
            return new State(state.hashCode() + action.hashCode() + 1);
        }

        // Retourne la liste des actions disponibles pour un état donné
        private List<String> available(Domain domain, State state) {
            // Logique pour récupérer les actions disponibles (à remplacer par la logique réelle)
            System.out.println("Getting available actions for state: " + state.hashCode());
            // Implémentation simplifiée : retourne toujours les mêmes actions
            return Arrays.asList("action1", "action2");
        }

        // Calcule la récompense pour une transition d'un état à un autre
        private double getReward(Specification spec, Domain domain, State state, String action, State nextState) {
            // Logique pour calculer la récompense (à remplacer par la logique réelle)
            System.out.println("Calculating reward for action: " + action);
            return 1.0; // Implémentation simplifiée
        }

        // Retourne le facteur de discount à appliquer
        private double getDiscount(Specification spec) {
            // Logique pour récupérer le facteur de discount (à remplacer par la logique réelle)
            return 0.99;
        }
    }

    // Classe représentant le domaine du problème
    public static class Domain {}

    // Classe représentant un état du problème
    public static class State {
        private int id;

        public State(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            State state = (State) obj;
            return id == state.id;
        }
    }

    // Classe représentant les spécifications du problème
    public static class Specification {}

    // Classe représentant une heuristique pour évaluer les états
    public static class Heuristic {
        public double compute(Domain domain, State state, Specification spec) {
            // Logique de calcul heuristique (à remplacer par la logique réelle)
            return 0.0; // Implémentation simplifiée
        }
    }

    // Classe PDDL avec une action no_op pour les transitions neutres
    public static class PDDL {
        public static String no_op = "no_op";
    }

    // Méthode principale pour exécuter le planificateur MCTS
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java MCTSPlanner <domain-file> <problem-file>");
            return;
        }

        String domainFile = args[0];
        String problemFile = args[1];

        // Charge le fichier de domaine et le fichier de problème
        Domain domain = loadDomain(domainFile);
        State initialState = loadProblem(problemFile);
        Specification spec = new Specification();

        // Exemple d'utilisation de MCTS
        MonteCarloTreeSearch mcts = new MonteCarloTreeSearch(500, 50, new Heuristic(), new RandomRolloutEstimator());
        MCTSTreeSolution solution = mcts.solve(domain, initialState, spec);
    }

    // Méthode pour charger le domaine à partir d'un fichier
    private static Domain loadDomain(String domainFile) {
        // Logique pour charger le domaine (à remplacer par la logique réelle)
        return new Domain(); // Implémentation simplifiée
    }

    // Méthode pour charger le problème à partir d'un fichier
    private static State loadProblem(String problemFile) {
        // Logique pour charger le problème (à remplacer par la logique réelle)
        return new State(problemFile.hashCode()); // Implémentation simplifiée
    }
}
