public abstract class Question {
    protected int questionId;
    protected String questionText;
    protected String[] options;
    protected String correctAnswer;

    public Question(int questionId, String questionText, String[] options, String correctAnswer) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    public abstract boolean validate(String answer);
    public abstract String getCorrectAnswer();
    public abstract void displayQuestion();
    
    public int getQuestionId() {
        return questionId;
    }
}

