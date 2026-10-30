/**
 * Abstract base for every kind of question. It holds the data all questions
 * share and leaves the type-specific behaviour (validation, display) to
 * subclasses, so the rest of the system only ever talks to a Question.
 */
public abstract class Question {
    protected int questionId;
    protected String questionText;
    protected String[] options;
    protected String correctAnswer;

    public Question(int questionId, String questionText, String[] options, String correctAnswer) {
        this.questionId = questionId;
        this.questionText = questionText;
        // Copy the array so later changes by the caller cannot alter this question.
        this.options = (options == null) ? null : options.clone();
        this.correctAnswer = correctAnswer;
    }

    public int getQuestionId() {
        return questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    /** Returns a copy, so callers cannot change the stored options. */
    public String[] getOptions() {
        return options.clone();
    }

    /** True if the given answer is an acceptable response to this question. */
    public abstract boolean validate(String answer);

    /** The stored correct answer, in the same form that validate() accepts. */
    public abstract String getCorrectAnswer();

    /** Prints the question and its options to the console. */
    public abstract void displayQuestion();
}
