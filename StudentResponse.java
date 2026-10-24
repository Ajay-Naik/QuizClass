import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * One student's answers for one quiz (identified by class code).
 * Answers are single letters, keyed by questionId. The score is never stored in
 * the file: it is recomputed from the answers and the quiz's answer key.
 * Methods that touch the shared state are synchronized because the watcher
 * thread (auto-submit) and the main thread can both reach the same response.
 */
public class StudentResponse {
    private int studentId;
    private String studentName;
    private String classCode;
    private Map<Integer, String> answers; // questionId -> letter, e.g. 1 -> "D"
    private int percentage;
    private boolean submitted;
    private long submittedTime;

    private static final String RESPONSE_FOLDER = "responses";

    public StudentResponse(int studentId, String studentName, String classCode) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.classCode = classCode;
        this.answers = new HashMap<>();
    }

    /**
     * Stores (or overwrites) the answer for a questionId. Not allowed after
     * submission.
     */
    public synchronized void submitAnswer(int questionId, String letter) {
        if (submitted) {
            throw new IllegalStateException("Already submitted.");
        }
        if (letter == null || letter.trim().isEmpty()) {
            throw new IllegalArgumentException("Answer cannot be empty.");
        }
        answers.put(questionId, letter.trim().toUpperCase());
    }

    /**
     * Compares each stored answer with the quiz's answer key (same questionId keys)
     * and sets the percentage.
     */
    public synchronized void calculateScore(Quiz quiz) {
        Map<Integer, String> key = quiz.getAnswerKey();
        int correct = 0;
        // Unanswered questions have no entry in 'answers', so they simply count as wrong.
        for (Map.Entry<Integer, String> entry : answers.entrySet()) {
            String right = key.get(entry.getKey());
            if (right != null && right.equalsIgnoreCase(entry.getValue())) {
                correct++;
            }
        }
        int total = quiz.getQuestionCount(); // divide by ALL questions, not just the answered ones
        percentage = (total == 0) ? 0 : (correct * 100) / total;
    }

    /** Locks the response (no more answers) and stamps the submission time once. */
    public synchronized void markSubmitted() {
        submitted = true;
        if (submittedTime == 0) {
            submittedTime = System.currentTimeMillis();
        }
    }

    public synchronized boolean isSubmitted() {
        return submitted;
    }

    /**
     * True if this student already has a saved (submitted) response for this class
     * code.
     */
    public static boolean savedResponseExists(String classCode, int studentId) {
        return Files.exists(responsePath(classCode, studentId));
    }

    /** Saves this response to responses/response_<classCode>_<studentId>.txt */
    public synchronized void saveToFile() throws IOException {
        Path file = responsePath(classCode, studentId);
        Files.createDirectories(file.getParent());
        List<String> lines = new ArrayList<>();
        lines.add("StudentId|" + studentId);
        lines.add("StudentName|" + clean(studentName));
        for (Map.Entry<Integer, String> e : new TreeMap<>(answers).entrySet()) {
            lines.add("Question|" + e.getKey() + "|Answer|" + e.getValue());
        }
        lines.add("SubmittedAt|" + getSubmittedTimeText());
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    /**
     * Reads a saved response back. Call calculateScore(quiz) afterwards to get the
     * percentage.
     */
    public static StudentResponse loadFromFile(String classCode, int studentId) throws IOException {
        Path file = responsePath(classCode, studentId);
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int id = studentId;
        String name = "";
        long time = 0;
        Map<Integer, String> saved = new TreeMap<>();
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] fields = line.split("\\|");
            try {
                switch (fields[0]) {
                    case "StudentId":
                        id = Integer.parseInt(fields[1]);
                        break;
                    case "StudentName":
                        name = (fields.length > 1) ? fields[1] : "";
                        break;
                    case "Question":
                        saved.put(Integer.parseInt(fields[1]), fields[3]);
                        break;           
                    case "SubmittedAt":
                        break;
                    default:
                        throw new IOException("Unknown line in response file: " + line);
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                throw new IOException("Bad line in response file: " + line);
            }
        }
        StudentResponse r = new StudentResponse(id, name, classCode);
        for (Map.Entry<Integer, String> e : saved.entrySet()) {
            r.submitAnswer(e.getKey(), e.getValue());
        }
        r.submittedTime = time;
        return r;
    }

    private static Path responsePath(String classCode, int studentId) {
        if (classCode == null || !classCode.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid class code for file name: " + classCode);
        }
        return Paths.get(RESPONSE_FOLDER, "response_" + classCode + "_" + studentId + ".txt");
    }

    /** '|' is the file separator, so keep it (and line breaks) out of names. */
    private static String clean(String text) {
        return (text == null) ? "" : text.replace('|', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    public String getSubmittedTimeText() {
        return java.time.ZonedDateTime
                .ofInstant(java.time.Instant.ofEpochMilli(submittedTime),
                        java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss z"));
    }

    public synchronized int getPercentage() {
        return percentage;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getClassCode() {
        return classCode;
    }

    public synchronized Map<Integer, String> getAnswers() {
        return new HashMap<>(answers);
    }

    @Override
    public synchronized String toString() {
        return studentName + " (ID " + studentId + ") - Quiz " + classCode + ": " + percentage + "%";
    }
}