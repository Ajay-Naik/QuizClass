/**
 * MCQ (multiple choice question): options are labelled A, B, C, D... in order,
 * and the correct answer is stored as a letter (e.g. "D").
 * This is the only concrete Question type; the rest of the system treats it
 * through the Question abstraction.
 */
public class MCQ extends Question {

    /**
     * @throws IllegalArgumentException if the text is blank, there are fewer than
     *                                  2 options, or the correct answer is not one
     *                                  of the option letters
     */
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

    /** True if the answer is a single option letter that exists for this question (A/B/C/D...). */
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

    /** A valid answer is exactly one letter that maps to an existing option index. */
    private boolean isValidOption(String answer) {
        String letter = normalize(answer);
        if (letter.length() != 1) {
            return false;
        }
        int index = letter.charAt(0) - 'A';
        return index >= 0 && index < options.length;
    }

    /** Trims and upper-cases so "d", " D " and "D" are treated the same; null becomes "". */
    private static String normalize(String s) {
        return (s == null) ? "" : s.trim().toUpperCase();
    }
}
