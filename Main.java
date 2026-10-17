public class Main {
    public static void main(String[] args) {
        QuizManager manager = new QuizManager();
        Teacher teacher = new Teacher("T1", "Ajay", "ajay@example.com", manager);

        Quiz quiz = teacher.createQuiz("Java OOP Quiz", 10);
        System.out.println("Quiz created. Class code: " + quiz.getClassCode());

        MCQ q = new MCQ(1, "What is OOP?",
                new String[]{"Inheritance", "Polymorphism", "Encapsulation", "All"}, "D");
        teacher.addQuestionToQuiz(quiz, q);

        System.out.println("Questions added: " + quiz.getQuestionCount());
        System.out.println("Answer key: " + quiz.getAnswerKey());
        System.out.println("Is D correct? " + q.isCorrect("d"));
        System.out.println("Is A correct? " + q.isCorrect("A"));
    }
}