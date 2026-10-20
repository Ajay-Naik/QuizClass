import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A student who joins quizzes by class code, answers questions and submits.
 */
public class Student {
    private int studentId;
    private String studentName;
    private String email;
    private List<StudentResponse> responses;

    private QuizManager quizManager; // used to look up quizzes by code
    private Quiz currentQuiz; // quiz currently being taken
    private StudentResponse currentResponse;

    public Student(int studentId, String studentName, String email, QuizManager quizManager) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.email = email;
        this.quizManager = quizManager;
        this.responses = new ArrayList<>();
    }

    /**
     * Joins a quiz using its class code. Throws
     * IllegalArgumentException/IllegalStateException on bad input.
     */
    public Quiz joinQuiz(String classCode) {
        if (classCode == null || classCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Class code is required.");
        }
        Quiz quiz = quizManager.getQuizByCode(classCode.trim());
        if (quiz == null) {
            throw new IllegalArgumentException("Invalid class code: " + classCode);
        }
        if (findResponseForQuiz(quiz.getQuizId()) != null) {
            throw new IllegalStateException("You have already submitted this quiz.");
        }
        currentQuiz = quiz;
        currentResponse = new StudentResponse(studentId, studentName, quiz.getQuizId());
        return quiz;
    }

    /**
     * Answers a question (1-based number) in the quiz that was joined. Rejects
     * answers the question considers invalid.
     */
    public void answerQuestion(int qNum, String answer) {
        if (currentQuiz == null) {
            throw new IllegalStateException("Join a quiz first.");
        }
        if (qNum < 1 || qNum > currentQuiz.getQuestionCount()) {
            throw new IllegalArgumentException("Question " + qNum + " does not exist.");
        }
        Question question = currentQuiz.getQuestions().get(qNum - 1);
        if (!question.validate(answer)) {
            throw new IllegalArgumentException("Invalid answer '" + answer
                    + "' for question " + qNum + ". Please choose one of the listed options.");
        }
        currentResponse.submitAnswer(qNum, answer);
    }

    /**
     * Grades the current response, saves it to a file and stores it. Also called by
     * the timer on auto-submit.
     */
    public StudentResponse submitQuiz() {
        if (currentQuiz == null) {
            throw new IllegalStateException("Join a quiz first.");
        }
        currentResponse.calculateScore(currentQuiz);
        StudentResponse submitted = currentResponse;
        responses.add(submitted);
        try {
            submitted.saveToFile(currentQuiz.getClassCode());
        } catch (IOException e) {
            System.out.println("Warning: could not save response file: " + e.getMessage());
        }
        currentQuiz = null;
        currentResponse = null;
        return submitted;
    }

    /**
     * Returns the number of correct answers for the quiz with this code, or -1 if
     * not taken.
     */
    public int viewScore(String classCode) {
        Quiz quiz = quizManager.getQuizByCode(classCode);
        if (quiz == null) {
            return -1;
        }
        StudentResponse r = findResponseForQuiz(quiz.getQuizId());
        return (r == null) ? -1 : r.getScore();
    }

    public List<StudentResponse> getAllResponses() {
        return new ArrayList<>(responses);
    }

    private StudentResponse findResponseForQuiz(int quizId) {
        for (StudentResponse r : responses) {
            if (r.getQuizId() == quizId) {
                return r;
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

    public String getEmail() {
        return email;
    }
}