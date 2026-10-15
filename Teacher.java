import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Teacher {
    private String teacherId;
    private String teacherName;
    private String email;
    private List<Quiz> quizzes = new ArrayList<>();
    private QuizManager manager;

    public Teacher(String teacherId, String teacherName, String email, QuizManager manager) {
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.email = email;
        this.manager = manager;
    }

    public Quiz createQuiz(String title, int duration) {
        String code = generateClassCode();
        Quiz quiz = new Quiz(quizzes.size() + 1, code, title, duration);
        quizzes.add(quiz);
        manager.createAndStoreQuiz(quiz);
        return quiz;
    }

    public void addQuestionToQuiz(Quiz quiz, Question q) {
        quiz.addQuestion(q);
    }

    public String generateClassCode() {
        Random rand = new Random();
        return String.valueOf(1000 + rand.nextInt(9000));
    }

    public Quiz getQuizByCode(String code) {
        return manager.getQuizByCode(code);
    }

    public void gradeAllResponses(String code) {
        // Implemented after StudentResponse is ready (integration)
    }

    public void saveQuizToFile(Quiz q) {
        // Implemented in Week 3 (file persistence)
    }
}