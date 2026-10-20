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
 * Stores one student's answers for one quiz, plus the graded result.
 * Can be saved to / loaded from a text file
 * (responses/response_<classCode>_<studentId>.txt).
 */
public class StudentResponse {
    private static final String RESPONSE_FOLDER = "responses";

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
        if (submittedTime == 0) { // keep the original time when re-grading a loaded response
            submittedTime = System.currentTimeMillis();
        }
    }

    /**
     * Saves this response to responses/response_<classCode>_<studentId>.txt:
     * StudentId|101
     * StudentName|Ajay
     * Question|1|Answer|D
     * SubmittedTime|1760000000000
     */
    public void saveToFile(String classCode) throws IOException {
        Path file = responsePath(classCode, studentId);
        Files.createDirectories(file.getParent());

        List<String> lines = new ArrayList<>();
        lines.add("StudentId|" + studentId);
        lines.add("StudentName|" + clean(studentName));
        for (Map.Entry<Integer, String> e : new TreeMap<>(answers).entrySet()) {
            lines.add("Question|" + e.getKey() + "|Answer|" + e.getValue());
        }
        lines.add("SubmittedTime|" + submittedTime);
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    /**
     * Reads a saved response back. The score fields are not stored in the file,
     * so call calculateScore(quiz) afterwards to get them.
     */
    public static StudentResponse loadFromFile(String classCode, int quizId, int studentId)
            throws IOException {
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
            String[] p = line.split("\\|");
            try {
                switch (p[0]) {
                    case "StudentId":
                        id = Integer.parseInt(p[1]);
                        break;
                    case "StudentName":
                        name = (p.length > 1) ? p[1] : "";
                        break;
                    case "Question":
                        saved.put(Integer.parseInt(p[1]), p[3]);
                        break;
                    case "SubmittedTime":
                        time = Long.parseLong(p[1]);
                        break;
                    default:
                        throw new IOException("Unknown line in response file: " + line);
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                throw new IOException("Bad line in response file: " + line);
            }
        }

        StudentResponse r = new StudentResponse(id, name, quizId);
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

    /**
     * The file format uses '|' as a separator, so keep it (and line breaks) out of
     * names.
     */
    private static String clean(String text) {
        return (text == null) ? "" : text.replace('|', ' ').replace('\n', ' ').replace('\r', ' ');
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