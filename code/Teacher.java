import java.io.IOException;
import java.util.Random;

/**
 * A teacher creates quizzes and reads their responses and results. Quizzes are
 * handed to the QuizManager, which is the single place they are stored.
 */
public class Teacher {
    private String teacherId;
    private String teacherName;
    private String email;
    private QuizManager manager;
    private final Random random = new Random();

    public Teacher(String teacherId, String teacherName, String email, QuizManager manager) {
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.email = email;
        this.manager = manager;
    }

    /** Creates a quiz with a fresh class code and registers it with the manager. */
    public Quiz createQuiz(String title, int duration) {
        String code = generateClassCode();
        while (manager.getQuizByCode(code) != null) { // never reuse a code
            code = generateClassCode();
        }
        Quiz quiz = new Quiz(nextQuizId(), code, title, duration);
        manager.createAndStoreQuiz(quiz);
        return quiz;
    }

    /** One more than the highest id in use, so ids stay unique after quizzes are loaded from files. */
    private int nextQuizId() {
        int next = 1;
        for (Quiz q : manager.getAllQuizzes()) {
            next = Math.max(next, q.getQuizId() + 1);
        }
        return next;
    }

    public void addQuestionToQuiz(Quiz quiz, Question q) {
        quiz.addQuestion(q);
    }

    /** A random 4-digit code (1000-9999). Uniqueness is enforced in createQuiz. */
    public String generateClassCode() {
        return String.valueOf(1000 + random.nextInt(9000));
    }

    /** Shows who has joined and how far they are. */
    public void viewLiveResponses(String code) {
        Quiz quiz = manager.getQuizByCode(code);
        if (quiz == null) {
            System.out.println("No quiz with code " + code);
            return;
        }
        System.out.println("Quiz open: " + quiz.isOpen());
        if (quiz.getResponses().isEmpty()) {
            System.out.println("No students have joined yet.");
        }
        for (StudentResponse r : quiz.getResponses()) {
            System.out.println(r.getStudentName() + " (ID " + r.getStudentId() + "): "
                    + r.getAnswers().size() + "/" + quiz.getQuestionCount() + " answered"
                    + (r.isSubmitted() ? " - submitted" : " - in progress"));
        }
    }

    /** Grades, marks as submitted and saves every response that has not been submitted yet. */
    public void gradeAllResponses(String code) {
        Quiz quiz = manager.getQuizByCode(code);
        if (quiz == null) {
            return;
        }
        for (StudentResponse r : quiz.getResponses()) {
            if (!r.isSubmitted()) {
                r.calculateScore(quiz);
                r.markSubmitted();
                try {
                    r.saveToFile();
                } catch (IOException e) {
                    System.out.println("Warning: could not save response of " + r.getStudentName());
                }
            }
        }
    }

    /** Prints each submitted student's percentage and the class average. */
    public void viewResults(String code) {
        Quiz quiz = manager.getQuizByCode(code);
        if (quiz == null) {
            System.out.println("No quiz with code " + code);
            return;
        }
        int count = 0;
        int total = 0;
        for (StudentResponse r : quiz.getResponses()) {
            if (r.isSubmitted()) {
                System.out.println(r.getStudentName() + " (ID " + r.getStudentId() + "): "
                        + r.getPercentage() + "%");
                total += r.getPercentage();
                count++;
            }
        }
        System.out.println(count == 0 ? "No submissions yet." : "Class average: " + (total / count) + "%");
    }

    public void saveQuizToFile(Quiz q) {
        try {
            q.saveToFile();
        } catch (IOException e) {
            System.out.println("Warning: could not save quiz " + q.getClassCode() + ": " + e.getMessage());
        }
    }

    /** Prints every quiz: code, title, length and question count. */
    public void listQuizzes() {
        if (manager.getAllQuizzes().isEmpty()) {
            System.out.println("No quizzes yet.");
        }
        for (Quiz q : manager.getAllQuizzes()) {
            System.out.println(q.getClassCode() + " - " + q.getTitle() + " (" + q.getDuration()
                    + " min, " + q.getQuestionCount() + " questions)"
                    + (q.isOpen() ? " [running]" : ""));
        }
    }
}
