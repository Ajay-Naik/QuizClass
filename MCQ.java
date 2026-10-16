/**
 * MCQ (multiple choice question): options are labelled A, B, C, D... in order,
 * and the correct answer is stored as a letter (e.g. "D").
 */
public class MCQ extends Question {

    public MCQ(int questionId, String questionText,
            String[] options, String correctAnswer) {
        super(questionId, questionText, options, normalize(correctAnswer));
        if (questionText == null || questionText.trim().isEmpty()) {
            throw new IllegalArgumentException("Question text cannot be empty.");
        }
        if (options == null || options.length < 2) {
            throw new IllegalArgumentException("A question needs at least 2 options.");
        }
        if (!isValidOption(this.correctAnswer)) {
            throw new IllegalArgumentException(
                    "Correct answer must be one of the option letters, got: " + correctAnswer);
        }
    }

    /**
     * Checks that the answer is a valid option letter for this question
     * (A/B/C/D...).
     */
    @Override
    public boolean validate(String answer) {
        return isValidOption(answer);
    }

    /** True if the given answer is the correct one (case-insensitive). */
    public boolean isCorrect(String answer) {
        return answer != null && correctAnswer.equals(normalize(answer));
    }

    @Override
    public String getCorrectAnswer() {
        return correctAnswer;
    }

    /** Prints the question and its options as A) ..., B) ... */
    @Override
    public void displayQuestion() {
        System.out.println("Q" + questionId + ". " + questionText);
        for (int i = 0; i < options.length; i++) {
            System.out.println("   " + (char) ('A' + i) + ") " + options[i]);
        }
    }

    private boolean isValidOption(String answer) {
        String a = normalize(answer);
        if (a.length() != 1) {
            return false;
        }
        int index = a.charAt(0) - 'A';
        return index >= 0 && index < options.length;
    }

    public int getQuestionId() {
        return questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String[] getOptions() {
        return options.clone();
    }

    private static String normalize(String s) {
        return (s == null) ? "" : s.trim().toUpperCase();
    }
}