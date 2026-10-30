import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central store of all quizzes, looked up by class code. Teacher and Student
 * both go through this class, so they always see the same Quiz objects.
 */
public class QuizManager {
    private Map<String, Quiz> quizzes = new HashMap<>();

    public void createAndStoreQuiz(Quiz q) {
        quizzes.put(q.getClassCode(), q);
    }

    /** Returns the quiz with this class code, or null if there is none. */
    public Quiz getQuizByCode(String code) {
        return quizzes.get(code);
    }

    public List<Quiz> getAllQuizzes() {
        return new ArrayList<>(quizzes.values());
    }

    /**
     * Loads every quiz saved in the quizzes folder, together with the responses
     * already saved for it. Damaged files are skipped with a warning.
     */
    public void loadQuizzesFromFile() {
        Path folder = Paths.get("quizzes");
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "quiz_*.txt")) {
            for (Path file : files) {
                try {
                    Quiz quiz = Quiz.loadFromFile(file);
                    quiz.loadSavedResponses();
                    quizzes.put(quiz.getClassCode(), quiz);
                } catch (IOException e) {
                    System.out.println("Warning: skipped " + file.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Warning: could not read the quizzes folder: " + e.getMessage());
        }
    }
}
