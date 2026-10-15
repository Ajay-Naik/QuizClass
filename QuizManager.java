import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizManager {
    private Map<String, Quiz> quizzes = new HashMap<>();

    public void createAndStoreQuiz(Quiz q) {
        quizzes.put(q.getClassCode(), q);
    }

    public Quiz getQuizByCode(String code) {
        return quizzes.get(code);
    }

    public List<Quiz> getAllQuizzes() {
        return new ArrayList<>(quizzes.values());
    }

    public void deleteQuiz(String code) {
        quizzes.remove(code);
    }

    public void loadQuizzesFromFile() {
        // Implemented in Week 3 (file persistence)
    }
}