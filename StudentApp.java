import java.util.Scanner;

/**
 * Console flow for a student: enter details, join with a class code, answer,
 * submit, see the score.
 * Contains no grading logic - it only calls Student.
 */
public class StudentApp {
    public static void main(String[] args) {
        // Teacher setup (same idea as Main), so there is a quiz to join.
        QuizManager manager = new QuizManager();
        Teacher teacher = new Teacher("T1", "Ajay", "ajay@example.com", manager);
        Quiz quiz = teacher.createQuiz("Java OOP Quiz", 10);
        teacher.addQuestionToQuiz(quiz, new MCQ(1, "What is OOP?",
                new String[] { "Inheritance", "Polymorphism", "Encapsulation", "All" }, "D"));
        teacher.addQuestionToQuiz(quiz, new MCQ(2, "What is Java?",
                new String[] { "Language", "Island", "Coffee", "A & C" }, "D"));
        teacher.addQuestionToQuiz(quiz, new MCQ(3, "Which keyword makes a class inherit?",
                new String[] { "implements", "extends", "super", "this" }, "B"));
        System.out.println("[Teacher setup] Quiz created. Class code: " + quiz.getClassCode());

        System.out.println();

        quiz.startQuiz();

        Scanner in = new Scanner(System.in);

        String classCode = ask(in, "Enter class code: ");

        if (manager.getQuizByCode(classCode) == null) {
            System.out.println("Invalid class code");
            return;
        }

        int studentId = askInt(in, "Enter student ID: ");
        String name = ask(in, "Enter your name: ");

        Student student = new Student(studentId, name);
        Quiz joined = student.joinQuiz(classCode, manager);
        if (joined == null) {
            System.out.println("Invalid class code");
            return;
        }

        classCode = classCode.trim();
        if (student.viewScore(classCode) != -1) {
            System.out.println("You have already taken this quiz. Your score: "
                    + student.viewScore(classCode) + "%");
            return;
        }
        System.out.println();
        System.out.println("Joined: " + joined.getTitle());
        System.out.println();

        for (Question q : joined.getQuestions()) {
            q.displayQuestion();
            while (true) {
                String letter = ask(in, "Your answer: ");
                try {
                    student.answerQuestion(classCode, q.getQuestionId(), letter);
                    break;
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                }
            }
            System.out.println();
        }

        StudentResponse result = student.submitQuiz(classCode);
        try {
            result.saveToFile();
        } catch (java.io.IOException e) {
            System.out.println("Warning: could not save your response: " + e.getMessage());
        }
        System.out.println("Quiz submitted.");
        System.out.println("Your score: " + student.viewScore(classCode) + "%");
    }

    private static String ask(Scanner in, String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) {
            System.out.println();
            System.out.println("No more input. Exiting.");
            System.exit(0);
        }
        return in.nextLine().trim();
    }

    private static int askInt(Scanner in, String prompt) {
        while (true) {
            String text = ask(in, prompt);
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }
}