import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One quiz: its questions, the answer key, a countdown Timer and the responses
 * of the students who joined. A quiz is identified by its class code.
 *
 * Lifecycle: created -> startQuiz() -> open until the timer runs out or
 * endQuiz() is called -> ended for good (it cannot be restarted in the same session).
 */
public class Quiz {
    private int quizId;
    private String classCode;
    private String title;
    private int duration; // minutes
    private List<Question> questions;
    private Map<Integer, String> answerKey; // questionId -> correct answer
    private volatile boolean isActive; // volatile: read by the watcher thread
    private volatile boolean hasEnded;
    private List<StudentResponse> responses = new ArrayList<>(); // one per student who joined

    private Timer timer;

    private static final String QUIZ_FOLDER = "quizzes";

    public Quiz(int quizId, String classCode, String title, int duration) {
        this.quizId = quizId;
        this.classCode = classCode;
        this.title = title;
        this.duration = duration;
        this.questions = new ArrayList<>();
        this.answerKey = new HashMap<>();
        this.isActive = false;
    }

    public int getQuizId() {
        return quizId;
    }

    /**
     * Adds a question and records its correct answer in the answer key.
     * Not allowed once the quiz has started, because that would change grades mid-quiz.
     */
    public void addQuestion(Question q) {
        if (isActive || hasEnded) {
            throw new IllegalStateException("Questions cannot be added after the quiz has started.");
        }
        if (answerKey.containsKey(q.getQuestionId())) {
            throw new IllegalArgumentException(
                    "Question ID " + q.getQuestionId() + " already exists in this quiz.");
        }
        questions.add(q);
        answerKey.put(q.getQuestionId(), q.getCorrectAnswer());
    }

    /**
     * Starts the countdown. Refused if the quiz already ended (by endQuiz() or because
     * the time ran out), is already running, or has no questions.
     */
    public void startQuiz() {
        if (hasEnded || (timer != null && !isOpen())) {
            throw new IllegalStateException("Quiz " + classCode + " has ended and cannot be restarted.");
        }
        if (isActive) {
            throw new IllegalStateException("Quiz " + classCode + " is already running.");
        }
        if (questions.isEmpty()) {
            throw new IllegalStateException("Quiz " + classCode + " has no questions and cannot be started.");
        }
        timer = new Timer();
        timer.startTimer(duration);
        isActive = true;
    }

    public long getRemainingSeconds() {
        if (timer == null) {
            return 0;
        }
        return timer.getRemainingTime() / 1000;
    }

    /** Ends the quiz now. Only a running quiz can be ended, and only once. */
    public void endQuiz() {
        if (hasEnded) {
            throw new IllegalStateException("Quiz " + classCode + " has already ended.");
        }
        if (!isActive) {
            throw new IllegalStateException("Quiz " + classCode
                    + " is not running. It has not been started in this session.");
        }
        if (timer != null) {
            timer.stopTimer();
        }
        isActive = false;
        hasEnded = true;
    }

    /**
     * True while students can answer: the quiz was started, was not ended, and the
     * timer has not run out. Time running out closes the quiz without any method call.
     */
    public boolean isOpen() {
        return isActive && timer != null && !timer.isTimeUp();
    }

    /** Remembers a student's response so the teacher can read it. A rejoin replaces the old entry. */
    public synchronized void registerResponse(StudentResponse r) {
        responses.removeIf(existing -> existing.getStudentId() == r.getStudentId());
        responses.add(r);
    }

    /**
     * Reads back the responses saved for this quiz in earlier runs, so the teacher can
     * still see results after a restart. Saved responses are always submitted ones.
     */
    public void loadSavedResponses() {
        Path folder = Paths.get("responses");
        if (!Files.isDirectory(folder)) {
            return;
        }
        // File names look like response_<classCode>_<studentId>.txt
        String prefix = "response_" + classCode + "_";
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, prefix + "*.txt")) {
            for (Path file : files) {
                String name = file.getFileName().toString();
                try {
                    int id = Integer.parseInt(name.substring(prefix.length(), name.length() - 4));
                    StudentResponse r = StudentResponse.loadFromFile(classCode, id);
                    r.calculateScore(this); // the score is not stored in the file, so recompute it
                    r.markSubmitted();
                    registerResponse(r);
                } catch (NumberFormatException | IOException e) {
                    System.out.println("Warning: skipped " + name + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Warning: could not read the responses folder: " + e.getMessage());
        }
    }

    /** Returns a copy of the list of responses, safe to loop over. */
    public synchronized List<StudentResponse> getResponses() {
        return new ArrayList<>(responses);
    }

    /**
     * Saves this quiz to quizzes/quiz_<classCode>.txt:
     * QUIZ_METADATA
     * QuizId|1|Title|Java OOP Quiz|Duration|10|Code|4729
     * QUESTIONS
     * questionId|text|correctLetter|optionA|optionB|...
     * Whether the quiz is running is not saved: a loaded quiz must be started again.
     */
    public void saveToFile() throws IOException {
        Path file = quizPath(classCode);
        Files.createDirectories(file.getParent());
        List<String> lines = new ArrayList<>();
        lines.add("QUIZ_METADATA");
        lines.add("QuizId|" + quizId + "|Title|" + clean(title) + "|Duration|" + duration
                + "|Code|" + classCode);
        lines.add("QUESTIONS");
        for (Question q : questions) {
            StringBuilder line = new StringBuilder();
            line.append(q.getQuestionId()).append('|').append(clean(q.getQuestionText()))
                    .append('|').append(q.getCorrectAnswer());
            for (String option : q.getOptions()) {
                line.append('|').append(clean(option));
            }
            lines.add(line.toString());
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    /**
     * Reads a quiz saved by saveToFile. Throws IOException if the file is damaged
     * (missing metadata, a short or malformed line, a duplicate question id...).
     * Only multiple-choice questions can be stored, so every question line becomes an MCQ.
     */
    public static Quiz loadFromFile(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        Quiz quiz = null;
        boolean inQuestions = false; // false while reading the metadata line, true after "QUESTIONS"
        for (String line : lines) {
            if (line.trim().isEmpty() || line.equals("QUIZ_METADATA")) {
                continue;
            }
            if (line.equals("QUESTIONS")) {
                inQuestions = true;
                continue;
            }
            try {
                String[] fields = line.split("\\|", -1); // -1 keeps trailing empty fields
                if (!inQuestions) {
                    // QuizId|<id>|Title|<title>|Duration|<minutes>|Code|<code>
                    quiz = new Quiz(Integer.parseInt(fields[1]), fields[7], fields[3],
                            Integer.parseInt(fields[5]));
                } else {
                    if (quiz == null) {
                        throw new IOException("Questions before metadata in " + file);
                    }
                    // <id>|<text>|<correct letter>|<option A>|<option B>|...
                    String[] options = Arrays.copyOfRange(fields, 3, fields.length);
                    quiz.addQuestion(new MCQ(Integer.parseInt(fields[0]), fields[1], options, fields[2]));
                }
            } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
                // NumberFormatException is an IllegalArgumentException, so bad numbers land here too
                throw new IOException("Bad line in quiz file " + file + ": " + line);
            }
        }
        if (quiz == null) {
            throw new IOException("No quiz metadata in " + file);
        }
        return quiz;
    }

    /** Builds the file path, rejecting codes that could escape the quizzes folder. */
    private static Path quizPath(String classCode) {
        if (classCode == null || !classCode.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid class code for file name: " + classCode);
        }
        return Paths.get(QUIZ_FOLDER, "quiz_" + classCode + ".txt");
    }

    /** '|' is the file separator, so keep it (and line breaks) out of the text. */
    private static String clean(String text) {
        return (text == null) ? "" : text.replace('|', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    public int getQuestionCount() {
        return questions.size();
    }

    /** Read-only view: questions can only be added through addQuestion. */
    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    /** Read-only view: the key can only change through addQuestion. */
    public Map<Integer, String> getAnswerKey() {
        return Collections.unmodifiableMap(answerKey);
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
}
