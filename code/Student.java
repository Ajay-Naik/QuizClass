import java.util.HashMap;
import java.util.Map;

/**
 * A student who joins quizzes by class code, answers questions by questionId
 * and submits. Answers are matched by questionId only, never by list position.
 *
 * The state-changing methods are synchronized: the auto-submit watcher thread and
 * the main (console) thread can call autoSubmit and answerQuestion/submitQuiz at
 * the same moment, and only one of them may finish the response.
 */
public class Student {
    private int studentId;
    private String studentName;
    private Map<String, StudentResponse> responses; // keyed by class code
    private Map<String, Quiz> joinedQuizzes; // quizzes joined, keyed by class code

    public Student(int studentId, String studentName) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.responses = new HashMap<>();
        this.joinedQuizzes = new HashMap<>();
    }

    /**
     * Looks the quiz up by class code and creates (or reloads) this student's response.
     * Returns null if there is no such quiz. It does NOT check whether the quiz is open:
     * the caller (Demo) decides that, so a student can still join a closed quiz to view
     * a saved score.
     */
    public Quiz joinQuiz(String classCode, QuizManager manager) {
        if (classCode == null) {
            return null;
        }
        String code = classCode.trim();
        Quiz quiz = manager.getQuizByCode(code);
        if (quiz == null) {
            return null;
        }

        joinedQuizzes.put(code, quiz);
        if (!responses.containsKey(code)) { // joining again keeps the existing response
            StudentResponse r = loadSavedOrNew(code, quiz);
            responses.put(code, r);
            quiz.registerResponse(r); // so the teacher can see this student
        }
        return quiz;
    }

    /**
     * Stores the answer letter for a questionId.
     *
     * @throws IllegalStateException    if the student has not joined or the quiz is closed
     * @throws IllegalArgumentException if the question does not exist or the letter is not
     *                                  a valid option (checked by Question.validate)
     */
    public synchronized void answerQuestion(String classCode, int questionId, String letter) {
        StudentResponse response = responses.get(classCode);
        if (response == null) {
            throw new IllegalStateException("You have not joined quiz " + classCode + ".");
        }
        Quiz quiz = joinedQuizzes.get(classCode);
        if (!quiz.isOpen()) {
            throw new IllegalStateException("Quiz " + classCode + " is closed. Time is up.");
        }
        Question question = findQuestion(quiz, questionId);
        if (question == null) {
            throw new IllegalArgumentException("Question " + questionId + " does not exist.");
        }
        if (!question.validate(letter)) {
            throw new IllegalArgumentException("Invalid answer '" + letter
                    + "' for question " + questionId + ". Please choose one of the listed options.");
        }
        response.submitAnswer(questionId, letter);
    }

    /**
     * Grades the response, marks it submitted and returns it.
     *
     * @throws IllegalStateException if not joined, already submitted, or the quiz is closed
     */
    public synchronized StudentResponse submitQuiz(String classCode) {
        StudentResponse response = responses.get(classCode);
        if (response == null) {
            throw new IllegalStateException("You have not joined quiz " + classCode + ".");
        }
        if (response.isSubmitted()) {
            throw new IllegalStateException("Already submitted.");
        }
        if (!joinedQuizzes.get(classCode).isOpen()) {
            throw new IllegalStateException("Quiz " + classCode + " is closed. Time is up.");
        }
        response.calculateScore(joinedQuizzes.get(classCode));
        response.markSubmitted();
        return response;
    }

    /**
     * Returns the percentage for this quiz, or -1 if the quiz has not been
     * submitted.
     */
    public synchronized int viewScore(String classCode) {
        StudentResponse response = responses.get(classCode);
        if (response == null || !response.isSubmitted()) {
            return -1;
        }
        return response.getPercentage();
    }

    /**
     * If this student already submitted this quiz in an earlier run, a response
     * file exists.
     * Load it and mark it submitted so the quiz cannot be taken again.
     */
    private StudentResponse loadSavedOrNew(String code, Quiz quiz) {
        try {
            if (StudentResponse.savedResponseExists(code, studentId)) {
                StudentResponse saved = StudentResponse.loadFromFile(code, studentId);
                saved.calculateScore(quiz);
                saved.markSubmitted();
                return saved;
            }
        } catch (java.io.IOException e) {
            System.out.println("Warning: could not read your saved response: " + e.getMessage());
        }
        return new StudentResponse(studentId, studentName, code);
    }

    /**
     * Called by the watcher thread when time runs out. Grades whatever was answered
     * (unanswered questions count as wrong). Returns null if there is nothing to submit
     * because the student already submitted.
     */
    public synchronized StudentResponse autoSubmit(String classCode) {
        StudentResponse response = responses.get(classCode);
        if (response == null || response.isSubmitted()) {
            return null;
        }
        response.calculateScore(joinedQuizzes.get(classCode));
        response.markSubmitted();
        return response;
    }

    private Question findQuestion(Quiz quiz, int questionId) {
        for (Question q : quiz.getQuestions()) {
            if (q.getQuestionId() == questionId) {
                return q;
            }
        }
        return null;
    }

    public String getStudentName() {
        return studentName;
    }

    public int getStudentId() {
        return studentId;
    }
}