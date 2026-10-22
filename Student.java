import java.util.HashMap;
import java.util.Map;

/**
 * A student who joins quizzes by class code, answers questions by questionId
 * and submits.
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

    /** Looks the quiz up by class code. Returns null if there is no such quiz. */
    public Quiz joinQuiz(String classCode, QuizManager manager) {
        if (classCode == null) {
            return null;
        }
        String code = classCode.trim();
        Quiz quiz = manager.getQuizByCode(code);
        if (quiz == null) {
            return null;
        }
        // TODO: timer check - refuse to join if the quiz is not open (depends on Timer
        // wiring).
        joinedQuizzes.put(code, quiz);
        if (!responses.containsKey(code)) { // joining again keeps the existing response
            responses.put(code, new StudentResponse(studentId, studentName, code));
        }
        return quiz;
    }

    /**
     * Stores the answer letter for a questionId. Throws if the answer cannot be
     * accepted.
     */
    public void answerQuestion(String classCode, int questionId, String letter) {
        StudentResponse response = responses.get(classCode);
        if (response == null) {
            throw new IllegalStateException("You have not joined quiz " + classCode + ".");
        }
        // TODO: timer check - reject answers if the quiz is closed (time up).
        Question question = findQuestion(joinedQuizzes.get(classCode), questionId);
        if (question == null) {
            throw new IllegalArgumentException("Question " + questionId + " does not exist.");
        }
        if (!question.validate(letter)) {
            throw new IllegalArgumentException("Invalid answer '" + letter
                    + "' for question " + questionId + ". Please choose one of the listed options.");
        }
        response.submitAnswer(questionId, letter);
    }

    /** Grades the response, marks it submitted and returns it. */
    public StudentResponse submitQuiz(String classCode) {
        StudentResponse response = responses.get(classCode);
        if (response == null) {
            throw new IllegalStateException("You have not joined quiz " + classCode + ".");
        }
        if (response.isSubmitted()) {
            throw new IllegalStateException("Already submitted.");
        }
        response.calculateScore(joinedQuizzes.get(classCode));
        response.markSubmitted();
        return response;
    }

    /**
     * Returns the percentage for this quiz, or -1 if the quiz has not been
     * submitted.
     */
    public int viewScore(String classCode) {
        StudentResponse response = responses.get(classCode);
        if (response == null || !response.isSubmitted()) {
            return -1;
        }
        return response.getPercentage();
    }

    private Question findQuestion(Quiz quiz, int questionId) {
        for (Question q : quiz.getQuestions()) {
            if (q.getQuestionId() == questionId) {
                return q;
            }
        }
        return null;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }
}