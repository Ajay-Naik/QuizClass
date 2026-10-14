import java.util.HashMap;
import java.util.Map;

/**
 * Stores one student's answers for one quiz, plus the graded result.
 */
public class StudentResponse {
    private int studentId;
    private String studentName;
    private int quizId;
    private Map<Integer, String> answers; // Q1 -> "D", Q2 -> "B"
    private long submittedTime;
    private int correctCount;
    private int totalQuestions;
    private int percentage;

    public StudentResponse(int studentId, String studentName, int quizId) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.quizId = quizId;
        this.answers = new HashMap<>();
    }

    /** Records (or overwrites) the answer for a question number. */
    public void submitAnswer(int questionNum, String answer) {
        if (questionNum <= 0) {
            throw new IllegalArgumentException("Invalid question number: " + questionNum);
        }
        if (answer == null || answer.trim().isEmpty()) {
            throw new IllegalArgumentException("Answer cannot be empty.");
        }
        answers.put(questionNum, answer.trim().toUpperCase());
    }

    /**
     * Compares this response with the quiz's answer key and fills in the score
     * fields.
     */
    public void calculateScore(Quiz quiz) {
        Map<Integer, String> key = quiz.getAnswerKey();
        totalQuestions = quiz.getQuestionCount();
        correctCount = 0;

        for (Map.Entry<Integer, String> entry : key.entrySet()) {
            String given = answers.get(entry.getKey());
            if (given != null && given.equalsIgnoreCase(entry.getValue())) {
                correctCount++;
            }
        }
        percentage = (totalQuestions == 0) ? 0 : (correctCount * 100) / totalQuestions;
        submittedTime = System.currentTimeMillis();
    }

    public int getScore() {
        return correctCount;
    }

    public int getPercentage() {
        return percentage;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public int getQuizId() {
        return quizId;
    }

    public Map<Integer, String> getAnswers() {
        return new HashMap<>(answers);
    }

    public long getSubmittedTime() {
        return submittedTime;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    @Override
    public String toString() {
        return studentName + " (ID " + studentId + ") - Quiz " + quizId + ": "
                + correctCount + "/" + totalQuestions + " (" + percentage + "%)";
    }
}