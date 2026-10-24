import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;

/**
 * One console program with a teacher menu and a student menu.
 * Both share the same QuizManager, so a quiz created in teacher mode
 * can be joined in student mode. Contains no grading logic: it only calls
 * Teacher, Student and Quiz.
 */
public class Demo {
    private static final Scanner input = new Scanner(System.in);
    private static final QuizManager manager = new QuizManager();
    private static final Teacher teacher = new Teacher("T1", "Ajay", "ajay@example.com", manager);

    public static void main(String[] args) {
        manager.loadQuizzesFromFile();
        System.out.println("Loaded " + manager.getAllQuizzes().size() + " saved quiz(zes).");
        while (true) {
            System.out.println();
            System.out.println("< QUIZCLASS >");
            System.out.println("1. Teacher mode");
            System.out.println("2. Student mode");
            System.out.println("3. Exit");
            switch (ask("Choice: ")) {
                case "1":
                    teacherMenu();
                    break;
                case "2":
                    studentMode();
                    break;
                case "3":
                    return;
                default:
                    System.out.println("Enter 1, 2 or 3.");
            }
        }
    }

    //  teacher

    private static void teacherMenu() {
        while (true) {
            System.out.println();
            System.out.println("* TEACHER *");
            System.out.println("1. Create quiz");
            System.out.println("2. Load sample quiz (3 questions)");
            System.out.println("3. List quizzes");
            System.out.println("4. Start quiz");
            System.out.println("5. View live responses");
            System.out.println("6. End quiz and grade");
            System.out.println("7. View results");
            System.out.println("8. Back");
            switch (ask("Choice: ")) {
                case "1":
                    createQuiz();
                    break;
                case "2":
                    loadSampleQuiz();
                    break;
                case "3":
                    teacher.listQuizzes();
                    break;
                case "4":
                    startQuiz();
                    break;
                case "5":
                    teacher.viewLiveResponses(ask("Class code: "));
                    break;
                case "6":
                    endAndGrade();
                    break;
                case "7":
                    teacher.viewResults(ask("Class code: "));
                    break;
                case "8":
                    return;
                default:
                    System.out.println("Enter a number from 1 to 8.");
            }
        }
    }

    /** Asks for the quiz details and every question, then saves the quiz to a file. */
    private static void createQuiz() {
        String title = askNonEmpty("Quiz title: ");
        int minutes = askPositiveInt("Duration (minutes): ");
        int count = askPositiveInt("Number of questions: ");
        Quiz quiz = teacher.createQuiz(title, minutes);
        for (int id = 1; id <= count; id++) {
            addQuestion(quiz, id);
        }
        teacher.saveQuizToFile(quiz);
        System.out.println("Quiz created and saved. Class code: " + quiz.getClassCode());
    }

    /** Reads one multiple-choice question; every field is re-asked until it is valid. */
    private static void addQuestion(Quiz quiz, int id) {
        String text = askNonEmpty("Q" + id + " text: ");
        String[] options = new String[4];
        for (int i = 0; i < options.length; i++) {
            options[i] = askNonEmpty("  Option " + (char) ('A' + i) + ": ");
        }

        String correct;
        while (true) {
            correct = ask("  Correct letter (A-D): ").toUpperCase();
            if (correct.matches("[A-D]")) {
                break;
            }
            System.out.println("Please enter A, B, C or D.");
        }

        teacher.addQuestionToQuiz(quiz, new MCQ(id, text, options, correct));
    }

    private static void loadSampleQuiz() {
        int minutes = askPositiveInt("Duration (minutes): ");
        Quiz quiz = teacher.createQuiz("Java OOP Quiz", minutes);
        teacher.addQuestionToQuiz(quiz, new MCQ(1, "What is OOP?",
                new String[] { "Inheritance", "Polymorphism", "Encapsulation", "All" }, "D"));
        teacher.addQuestionToQuiz(quiz, new MCQ(2, "What is Java?",
                new String[] { "Language", "Island", "Coffee", "A & C" }, "D"));
        teacher.addQuestionToQuiz(quiz, new MCQ(3, "Which keyword makes a class inherit?",
                new String[] { "implements", "extends", "super", "this" }, "B"));
        teacher.saveQuizToFile(quiz);
        System.out.println("Sample quiz created and saved. Class code: " + quiz.getClassCode());
    }

    private static void startQuiz() {
        Quiz quiz = findQuiz(ask("Class code: "));
        if (quiz == null) {
            return;
        }
        try {
            quiz.startQuiz();
            System.out.println("Quiz started. Students can join now ("
                    + quiz.getDuration() + " minute(s)).");
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage()); // already running, already ended, or no questions
        }
    }

    /** Ends the quiz, then grades and saves any response that was still unsubmitted. */
    private static void endAndGrade() {
        String code = ask("Class code: ");
        Quiz quiz = findQuiz(code);
        if (quiz == null) {
            return;
        }
        try {
            quiz.endQuiz();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
            return;
        }
        teacher.gradeAllResponses(code);
        System.out.println("Quiz ended. All responses graded.");
    }

    private static Quiz findQuiz(String code) {
        Quiz quiz = manager.getQuizByCode(code);
        if (quiz == null) {
            System.out.println("No quiz with code " + code);
        }
        return quiz;
    }

    //  student

    /**
     * The whole student session: join, answer every question, submit, see the score.
     * A background watcher thread auto-submits the moment the quiz closes, so the
     * main thread (blocked reading input) never has to notice the deadline itself.
     */
    private static void studentMode() {
        String code = ask("Enter class code: ");
        Quiz found = manager.getQuizByCode(code);
        if (found == null) {
            System.out.println("Invalid class code");
            return;
        }
        int studentId = askInt("Enter student ID: ");
        String name = ask("Enter your name: ");

        // A closed quiz is refused only if this student has no saved result to view.
        if (!found.isOpen() && !StudentResponse.savedResponseExists(code, studentId)) {
            System.out.println("This quiz is not open (not started yet, or time is up).");
            return;
        }

        final Student student = new Student(studentId, name);
        final Quiz quiz = student.joinQuiz(code, manager);
        if (quiz == null) {
            System.out.println("Invalid class code");
            return;
        }
        // A response reloaded from its file is already submitted: show the score, do not retake.
        if (student.viewScore(code) != -1) {
            System.out.println("You have already taken this quiz. Your score: "
                    + student.viewScore(code) + "%");
            return;
        }

        // Watcher: polls every 500 ms; when the quiz closes it auto-submits what was answered.
        final AtomicBoolean timedOut = new AtomicBoolean(false);
        Thread watcher = new Thread(() -> {
            try {
                while (quiz.isOpen()) {
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                return; // the student submitted normally, so the watcher is no longer needed
            }
            StudentResponse auto = student.autoSubmit(code); // null if already submitted
            if (auto != null) {
                timedOut.set(true);
                saveResponse(auto);
                System.out.println();
                System.out.println("Time is up. Your quiz was auto-submitted. Press Enter to continue.");
                System.out.println("Submitted at: " + auto.getSubmittedTimeText());
                System.out.println("Your score: " + auto.getPercentage() + "%");
            }
        });
        watcher.setDaemon(true); // never keeps the program alive on its own
        watcher.start();

        System.out.println();
        System.out.println("Joined: " + quiz.getTitle());

        // Ask each question until it gets a valid answer (or time runs out).
        for (Question q : quiz.getQuestions()) {
            if (timedOut.get() || !quiz.isOpen()) {
                break;
            }
            System.out.println();
            System.out.println("Time left: " + formatTime(quiz.getRemainingSeconds()));
            q.displayQuestion();
            while (!timedOut.get()) {
                String letter = askUnlessClosed("Your answer: ", quiz);
                if (letter == null) {
                    break; // quiz closed while waiting for input
                }
                try {
                    student.answerQuestion(code, q.getQuestionId(), letter);
                    break;
                } catch (IllegalStateException | IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                    if (!quiz.isOpen()) {
                        break;
                    }
                }
            }
        }

        if (timedOut.get()) {
            return; // the watcher already graded and printed the score
        }

        // All questions answered: submit manually. If the quiz closed at the last moment,
        // submitQuiz refuses and the answers given so far are auto-submitted instead.
        StudentResponse result;
        boolean late = false;
        try {
            result = student.submitQuiz(code);
        } catch (IllegalStateException e) {
            result = student.autoSubmit(code);
            late = true;
        }
        if (result == null) { // the watcher got there first and is printing the result
            try {
                watcher.join();
            } catch (InterruptedException ignored) {
            }
            return;
        }
        watcher.interrupt();
        saveResponse(result);
        System.out.println();
        System.out.println(late ? "Time is up. Your quiz was auto-submitted." : "Quiz submitted.");
        System.out.println("Submitted at: " + result.getSubmittedTimeText());
        System.out.println("Your score: " + result.getPercentage() + "%");
    }

    private static void saveResponse(StudentResponse r) {
        try {
            r.saveToFile();
        } catch (java.io.IOException e) {
            System.out.println("Warning: could not save your response: " + e.getMessage());
        }
    }

    //  input

    /** Prints the prompt and returns the trimmed line. Exits cleanly if input ends. */
    private static String ask(String prompt) {
        System.out.print(prompt);
        if (!input.hasNextLine()) {
            System.out.println();
            System.out.println("No more input. Exiting.");
            System.exit(0);
        }
        return input.nextLine().trim();
    }

    /** Like ask(), but returns null as soon as the quiz closes, so no Enter is needed. */
    private static String askUnlessClosed(String prompt, Quiz quiz) {
        System.out.print(prompt);
        try {
            while (System.in.available() == 0) {
                if (!quiz.isOpen()) {
                    return null;
                }
                Thread.sleep(100);
            }
        } catch (IOException | InterruptedException e) {
            // fall through to a normal blocking read
        }
        if (!input.hasNextLine()) {
            System.out.println();
            System.out.println("No more input. Exiting.");
            System.exit(0);
        }
        return input.nextLine().trim();
    }

    /** Re-asks until the answer is not blank. */
    private static String askNonEmpty(String prompt) {
        while (true) {
            String text = ask(prompt);
            if (!text.isEmpty()) {
                return text;
            }
            System.out.println("This cannot be empty.");
        }
    }

    private static int askInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(ask(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private static int askPositiveInt(String prompt) {
        while (true) {
            int n = askInt(prompt);
            if (n >= 1) {
                return n;
            }
            System.out.println("Enter 1 or more.");
        }
    }

    private static String formatTime(long seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
