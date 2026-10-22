import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Quiz {
    private int quizId;
    private String classCode;
    private String title;
    private int duration; // minutes
    private List<Question> questions;
    private Map<Integer, String> answerKey;
    private boolean isActive;
    private long createdTime;

    private Timer timer; 

    public Quiz(int quizId, String classCode, String title, int duration) {
        this.quizId = quizId;
        this.classCode = classCode;
        this.title = title;
        this.duration = duration;
        this.questions = new ArrayList<>();
        this.answerKey = new HashMap<>();
        this.isActive = false;
        this.createdTime = System.currentTimeMillis();
    }

    public int getQuizId() {
        return quizId;
    }

    public void addQuestion(Question q) {
    if (answerKey.containsKey(q.getQuestionId())) {
        throw new IllegalArgumentException(
                "Question ID " + q.getQuestionId() + " already exists in this quiz.");
    }
    questions.add(q);
    answerKey.put(q.getQuestionId(), q.getCorrectAnswer());
}

    public void startQuiz() {
    if (isActive) {
        throw new IllegalStateException("Quiz " + classCode + " is already running.");
    }
    timer = new Timer();
    timer.startTimer(duration);
    isActive = true;
    }   

    public void endQuiz() {
    if (timer != null) {
        timer.stopTimer();
    }
    isActive = false;
    }
    
    public boolean isOpen() {
        return isActive && timer != null && !timer.isTimeUp();
    }

    public int getQuestionCount() {
        return questions.size();
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public Map<Integer, String> getAnswerKey() {
        return answerKey;
    }

    public String getClassCode() {
        return classCode;
    }

    public String getTitle() {
        return title;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isActive() {
        return isActive;
    }
}